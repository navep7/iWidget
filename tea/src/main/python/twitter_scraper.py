import sys
import os
import threading
from types import ModuleType

# ==============================================================================
# ROBUST ANDROID MULTIPROCESSING PATCH (v12)
# ==============================================================================

def patch_multiprocessing_for_android():
    # 1. Force clear all multiprocessing modules to ensure a fresh, consistent patch.
    # Chaquopy persists the interpreter state; this clears any previous broken states.
    for name in list(sys.modules.keys()):
        if "multiprocessing" in name or name == "_multiprocessing":
            if sys.modules[name] is None or not hasattr(sys.modules[name], "__file__"):
                del sys.modules[name]

    # 2. Remove any custom MetaPathFinders from previous attempts.
    sys.meta_path = [f for f in sys.meta_path if not any(x in type(f).__name__ for x in ['Blocker', 'Sync', 'Multiprocessing'])]

    # 3. Universal Constants
    SEM_VALUE_MAX = 2147483647
    def android_cpu_count(): return 4
    os.cpu_count = android_cpu_count

    # 4. Robust Wrapper Classes
    # These fix "TypeError: lock() takes no keyword arguments" by swallowing extra args like 'ctx'.
    class RobustSyncBase:
        def __enter__(self, *args, **kwargs): return self._obj.__enter__()
        def __exit__(self, *args, **kwargs): return self._obj.__exit__(*args, **kwargs)
        
        def acquire(self, *args, **kwargs):
            # threading primitives take zero or limited positional args for acquire.
            # We ignore all arguments to ensure it never crashes on Android.
            return self._obj.acquire()
                
        def release(self, *args, **kwargs): return self._obj.release()
        def __getattr__(self, name): return getattr(self._obj, name)

    class RobustLock(RobustSyncBase):
        def __init__(self, *args, **kwargs): self._obj = threading.RLock()

    class RobustRLock(RobustSyncBase):
        def __init__(self, *args, **kwargs): self._obj = threading.RLock()

    class RobustSemaphore(RobustSyncBase):
        def __init__(self, value=1, *args, **kwargs):
            val = kwargs.get("value", args[0] if args else value)
            self._obj = threading.Semaphore(val)

    class RobustBoundedSemaphore(RobustSyncBase):
        def __init__(self, value=1, *args, **kwargs):
            val = kwargs.get("value", args[0] if args else value)
            self._obj = threading.BoundedSemaphore(val)

    # 5. Mock native backend (_multiprocessing)
    _mp = ModuleType("_multiprocessing")
    _mp.SEM_VALUE_MAX = SEM_VALUE_MAX
    _mp.SemLock = RobustLock
    sys.modules["_multiprocessing"] = _mp

    # 6. Mock multiprocessing.synchronize (Fixes SEM_VALUE_MAX ImportError)
    sync = ModuleType("multiprocessing.synchronize")
    sync.SEM_VALUE_MAX = SEM_VALUE_MAX
    sync.Lock = RobustLock
    sync.RLock = RobustRLock
    sync.Semaphore = RobustSemaphore
    sync.BoundedSemaphore = RobustBoundedSemaphore
    sync.Condition = lambda lock=None, **k: threading.Condition(lock)
    sync.Event = lambda **k: threading.Event()
    sync.Barrier = lambda parties, action=None, timeout=None, **k: threading.Barrier(parties, action, timeout)
    sync.SemLock = RobustLock
    sync.__all__ = ["SEM_VALUE_MAX", "Lock", "RLock", "Semaphore", "BoundedSemaphore", "Condition", "Event", "Barrier", "SemLock"]
    sys.modules["multiprocessing.synchronize"] = sync

    # 7. Patch dummy variant
    import multiprocessing.dummy as dummy
    dummy.cpu_count = android_cpu_count
    dummy.Lock = RobustLock
    dummy.RLock = RobustRLock
    dummy.Semaphore = RobustSemaphore
    dummy.BoundedSemaphore = RobustBoundedSemaphore
    sys.modules["multiprocessing.dummy"] = dummy

    # 8. Mock main multiprocessing package
    mp = ModuleType("multiprocessing")
    mp.__path__ = [] # Mark as package
    mp.cpu_count = android_cpu_count
    mp.synchronize = sync
    mp.SEM_VALUE_MAX = SEM_VALUE_MAX
    mp.Pool = dummy.Pool
    mp.Process = dummy.Process
    mp.Queue = dummy.Queue
    mp.Lock = RobustLock
    mp.RLock = RobustRLock
    mp.Semaphore = RobustSemaphore
    mp.BoundedSemaphore = RobustBoundedSemaphore
    mp.Condition = sync.Condition
    mp.Event = sync.Event
    mp.Barrier = sync.Barrier
    mp.current_process = dummy.current_process
    mp.get_context = lambda *a, **k: mp
    mp.dummy = dummy

    # Register all variants in sys.modules
    sys.modules["multiprocessing"] = mp
    sys.modules["multiprocessing.pool"] = dummy
    sys.modules["multiprocessing.queues"] = dummy

# Run the patch
patch_multiprocessing_for_android()

# ==============================================================================
# Now safely import ntscraper
# ==============================================================================
scraper_ready = False
init_err_msg = "Unknown Error"

try:
    from ntscraper import Nitter
    scraper_ready = True
except Exception as e_init:
    scraper_ready = False
    init_err_msg = str(e_init)

def get_user_tweets(username, count=20):
    """
    Scrapes public tweets from a user timeline using public Nitter instances.
    """
    if not scraper_ready:
        return [f"Scraper initialization failed: {init_err_msg}"]

    try:
        scraper = Nitter()
        # ntscraper will now safely use threads for any parallel tasks via our patch.
        data = scraper.get_tweets(username, mode='user', number=count)
        
        if not data or not isinstance(data, dict):
            return ["Error: Nitter returned invalid data. The instance might be down."]
            
        tweets = data.get('tweets', [])
        tweet_list = []
        for tweet in tweets:
            text = tweet.get('text')
            if text:
                tweet_list.append(text)
        
        if not tweet_list:
            return ["No tweets found for this user. Nitter instances are often rate-limited."]
            
        return tweet_list
    except Exception as e_run:
        # Capture error message to a string to avoid Python 3 scope issues
        run_err_msg = str(e_run)
        return [f"Python Execution Error: {run_err_msg}"]
