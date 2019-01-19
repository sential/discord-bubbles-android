package com.sential.discordbubbles

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import kotlinx.android.synthetic.main.chat_head_content.view.*
import kotlinx.android.synthetic.main.chat_message.view.*

class Content(context: Context): LinearLayout(context) {
    private val springSystem = SpringSystem.create()
    private val scaleSpring = springSystem.createSpring()

    private var channelView: TextView
    private var serverView: TextView
    private var hashTagView: TextView
    private var scrollView: ScrollView

    var messagesView: RelativeLayout

    var lastId = 0

    var channel: String? = null
        set(value) {
            if (value == null) {
                hashTagView.visibility = View.GONE
                serverView.visibility = View.GONE
                channelView.text = server
            } else {
                serverView.visibility = View.VISIBLE
                hashTagView.visibility = View.VISIBLE
                channelView.text = value
                serverView.text = server
            }
            field = value
        }

    var server: String? = null
        set(value) {
            when {
                value == null -> {
                    if (channel == null) {
                        channelView.visibility = View.GONE
                    }
                    serverView.visibility = View.GONE
                }
                channel == null -> {
                    serverView.visibility = View.GONE
                    channelView.text = value
                }
                else -> {
                    serverView.visibility = View.VISIBLE
                    serverView.text = value
                }
            }

            field = value
        }

    init {
        inflate(context, R.layout.chat_head_content, this)

        channelView = findViewById(R.id.channel)
        serverView = findViewById(R.id.server)
        hashTagView = findViewById(R.id.hashtag)
        messagesView = findViewById(R.id.messages)
        scrollView = findViewById(R.id.scrollView)

        scaleSpring.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring) {
                scaleX = spring.currentValue.toFloat()
                scaleY = spring.currentValue.toFloat()
            }
        })
        scaleSpring.springConfig = SpringConfigs.CONTENT_SCALE

        scaleSpring.currentValue = 0.0
    }

    fun addMessage(message: Message) {
        val view = inflate(context, R.layout.chat_message, null)
        val root: LinearLayout = view.findViewById(R.id.root)
        root.id = View.generateViewId()

        val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)

        if (messagesView.childCount > 0) {
            val prev = messagesView.getChildAt(messagesView.childCount - 1)
            params.addRule(RelativeLayout.BELOW, prev.id)
            params.topMargin = WindowManagerHelper.dpToPx(4f)
            root.layoutParams = params
        } else {
            params.topMargin = WindowManagerHelper.dpToPx(16f)
            root.layoutParams = params
        }

        val body: TextView = view.findViewById(R.id.body)
        body.text = message.body

        messages.addView(view)

        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }

        lastId++
    }

    fun hideContent() {
        scaleSpring.endValue = 0.0

        val anim = AlphaAnimation(1.0f, 0.0f)
        anim.duration = 200
        anim.repeatMode = Animation.RELATIVE_TO_SELF
        startAnimation(anim)
    }

    fun showContent() {
        scaleSpring.endValue = 1.0

        val anim = AlphaAnimation(0.0f, 1.0f)
        anim.duration = 100
        anim.repeatMode = Animation.RELATIVE_TO_SELF
        startAnimation(anim)
    }
}