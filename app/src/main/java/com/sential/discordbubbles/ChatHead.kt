package com.sential.discordbubbles

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.*
import com.facebook.rebound.*
import kotlin.math.pow

class ChatHead(var chatHeads: ChatHeads, var guildInfo: GuildInfo): View(chatHeads.context), View.OnTouchListener, SpringListener {
    var isTop: Boolean = false
    var isActive: Boolean = false

    var params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManagerHelper.getLayoutFlag(),
        0,
        PixelFormat.TRANSLUCENT
    )

    var springSystem: SpringSystem = SpringSystem.create()

    var springX: Spring = springSystem.createSpring()
    var springY: Spring = springSystem.createSpring()

    private val paint = Paint()

    val messages = mutableListOf<Message>()

    private var initialX = 0.0f
    private var initialY = 0.0f

    private var initialTouchX = 0.0f
    private var initialTouchY = 0.0f

    private var moving = false

    override fun onSpringEndStateChange(spring: Spring?) = Unit
    override fun onSpringAtRest(spring: Spring?) = Unit
    override fun onSpringActivate(spring: Spring?) = Unit

    init {
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        params.width = ChatHeads.CHAT_HEAD_SIZE + 15
        params.height = ChatHeads.CHAT_HEAD_SIZE + 30

        springX.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring) {
                x = spring.currentValue.toFloat()
            }
        })
        springX.springConfig = SpringConfigs.NOT_DRAGGING
        springX.addListener(this)

        springY.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring) {
                y = spring.currentValue.toFloat()
            }
        })
        springY.springConfig = SpringConfigs.NOT_DRAGGING
        springY.addListener(this)

        this.setLayerType(View.LAYER_TYPE_HARDWARE, paint)

        chatHeads.addView(this, params)

        this.setOnTouchListener(this)

        guildInfo.onAvatarChange = {
            invalidate()
        }
    }

    override fun onSpringUpdate(spring: Spring) {
        if (spring !== this.springX && spring !== this.springY) return
        val totalVelocity = Math.hypot(springX.velocity, springY.velocity).toInt()

        chatHeads.onChatHeadSpringUpdate(this, spring, totalVelocity)
    }

    override fun onDraw(canvas: Canvas?) {
        if (guildInfo.chatHeadBitmap != null) {
            canvas?.drawBitmap(guildInfo.chatHeadBitmap!!, 0f, 0f, paint)
        }
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val currentChatHead = chatHeads.chatHeads.find { it == v }!!

        val metrics = WindowManagerHelper.getScreenSize()

        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = x
                initialY = y
                initialTouchX = event.rawX
                initialTouchY = event.rawY

                scaleX = 0.9f
                scaleY = 0.9f
            }
            MotionEvent.ACTION_UP -> {
                if (!moving) {
                    if (currentChatHead.isActive) {
                        chatHeads.collapse()
                    } else {
                        val selectedChatHead = chatHeads.chatHeads.find { it.isActive }!!
                        selectedChatHead.isActive = false
                        currentChatHead.isActive = true

                        chatHeads.updateActiveContent()
                    }
                } else {
                    springX.endValue = metrics.widthPixels - width - chatHeads.chatHeads.indexOf(this) * (width + ChatHeads.CHAT_HEAD_EXPANDED_PADDING).toDouble()
                    springY.endValue = ChatHeads.CHAT_HEAD_EXPANDED_MARGIN_TOP.toDouble()

                    if (isActive) {
                        chatHeads.content.showContent()
                    }
                }

                scaleX = 1f
                scaleY = 1f

                moving = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (ChatHeads.distance(initialTouchX, event.rawX, initialTouchY, event.rawY) > ChatHeads.CHAT_HEAD_DRAG_TOLERANCE.pow(2) && !moving) {
                    moving = true

                    if (isActive) {
                        chatHeads.content.hideContent()
                    }
                }

                if (moving) {
                    springX.currentValue = initialX + (event.rawX - initialTouchX).toDouble()
                    springY.currentValue = initialY + (event.rawY - initialTouchY).toDouble()
                }
            }
        }

        return true
    }
}