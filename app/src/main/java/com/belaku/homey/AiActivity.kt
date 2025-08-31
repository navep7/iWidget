package com.belaku.homey


import AppsAdapter
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.text.method.ScrollingMovementMethod
import android.transition.Fade
import android.transition.Slide
import android.view.KeyEvent
import android.view.Window
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.belaku.homey.MainActivity.Companion.apps
import com.belaku.homey.MainActivity.Companion.imgDescs
import com.belaku.homey.MainActivity.Companion.imgUrls
import com.belaku.homey.MainActivity.Companion.pD
import com.belaku.homey.MainActivity.Companion.queryType
import com.belaku.homey.MainActivity.Companion.sN
import com.belaku.homey.MainActivity.Companion.sharedPreferencesEditor
import com.belaku.homey.databinding.ActivityAiBinding
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class AiActivity : AppCompatActivity(), AppsAdapter.RvEvent {


    private lateinit var prompt: String
    private lateinit var txAi: TextView
    private lateinit var edtxAi: EditText
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityAiBinding

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAiBinding.inflate(layoutInflater)

        setContentView(binding.root)


        val rootLayout = findViewById<RelativeLayout>(R.id.ai_layout)
        rootLayout.setBackgroundDrawable(BitmapDrawable(getResources(), blur(applicationContext, SetWallWorker.wallBitmap)))


        edtxAi = findViewById<EditText>(R.id.edtx_ai)
        txAi = findViewById<TextView>(R.id.tx_ai_response)
        txAi.movementMethod = ScrollingMovementMethod()



            val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel("gemini-2.5-flash")




        edtxAi.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if ((event != null && (event.keyCode == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                //do what you want on the press of 'done'
                prompt = edtxAi.text.toString()
                txAi.setText("Generating AI response, please wait...")
                lifecycleScope.launch {
                    val txtResponse = generativeModel.generateContent(prompt)
                    Toast.makeText(applicationContext, txtResponse.text, Toast.LENGTH_LONG).show()
                    txAi.setText(txtResponse.text)
                }
            }
            false
        })





    }

    fun blur(context: Context?, image: Bitmap): Bitmap {

        var BITMAP_SCALE = 0.4f; // Scale down bitmap for performance
        var BLUR_RADIUS = 25f; // Adjust blur intensity

        val width = Math.round(image.width * BITMAP_SCALE).toInt()
        val height = Math.round(image.height * BITMAP_SCALE).toInt()

        val inputBitmap = Bitmap.createScaledBitmap(image, width, height, false)
        val outputBitmap = Bitmap.createBitmap(inputBitmap)

        val rs = RenderScript.create(context)
        val theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        val tmpIn = Allocation.createFromBitmap(rs, inputBitmap)
        val tmpOut = Allocation.createFromBitmap(rs, outputBitmap)

        theIntrinsic.setRadius(BLUR_RADIUS)
        theIntrinsic.setInput(tmpIn)
        theIntrinsic.forEach(tmpOut)
        tmpOut.copyTo(outputBitmap)

        return outputBitmap
    }

    override fun onItemClick(pos: Int) {
        val launchIntent = packageManager.getLaunchIntentForPackage(apps[pos].pName)
        startActivity(launchIntent)
    }

}