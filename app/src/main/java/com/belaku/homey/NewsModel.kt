package com.belaku.homey

class NewsModel {

    lateinit var author: String
    lateinit var title: String
    lateinit var description: String
    lateinit var url: String
    lateinit var urlToImage: String
    lateinit var publishedAt: String


    constructor(
        description: String,
        author: String,
        title: String,
        url: String,
        urlToImage: String,
        publishedAt: String
    ) {
        this.description = description
        this.author = author
        this.title = title
        this.url = url
        this.urlToImage = urlToImage
        this.publishedAt = publishedAt
    }


}
