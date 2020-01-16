package com.sential.discordbubbles.chatheads

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.view.*
import android.widget.*
import com.facebook.rebound.*
import com.sential.discordbubbles.R
import com.sential.discordbubbles.client.*
import com.sential.discordbubbles.utils.*
import net.dv8tion.jda.api.entities.Message
import kotlin.math.pow

class ChatHead(var chatHeads: ChatHeads, var guildInfo: GuildInfo): FrameLayout(chatHeads.context), View.OnTouchListener, SpringListener {
    var params: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        getOverlayFlag(),
        0,
        PixelFormat.TRANSLUCENT
    )

    var springSystem: SpringSystem = SpringSystem.create()

    var springX: Spring = springSystem.createSpring()
    var springY: Spring = springSystem.createSpring()

    private val paint = Paint()

    private var initialX = 0.0f
    private var initialY = 0.0f

    private var initialTouchX = 0.0f
    private var initialTouchY = 0.0f

    private var moving = false

    // TODO: move badge to right when necessary
    private var notificationsTextView: TextView
    private var notificationsView: LinearLayout

    var baseHistoryLoaded = false

    var notifications = 0
    set(value) {
        if (value >= 0) field = value

        if (value == 0) {
            notificationsView.visibility = GONE
        } else if (value > 0) {
            notificationsView.visibility = VISIBLE
            notificationsTextView.text = "$value"
        }
    }

    var messages = mutableListOf<MessageInfo>()

    private var avatarsCache = mutableMapOf<String, Bitmap?>()

    fun cacheAvatar(user: UserInfo): Bitmap? {
        return if (avatarsCache[user.avatarId] == null) {
            val bmp = fetchBitmap(user.avatarUrl)?.makeCircular()
            avatarsCache[user.avatarId] = bmp
            bmp
        } else {
            avatarsCache[user.avatarId]
        }
    }

    override fun onSpringEndStateChange(spring: Spring?) = Unit
    override fun onSpringAtRest(spring: Spring?) = Unit
    override fun onSpringActivate(spring: Spring?) = Unit

    init {
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        params.width = ChatHeads.CHAT_HEAD_SIZE + 15
        params.height = ChatHeads.CHAT_HEAD_SIZE + 30

        val view = inflate(context, R.layout.bubble, this)

        val imageView: ImageView = view.findViewById(R.id.bubble_avatar)
        notificationsTextView = view.findViewById(R.id.bubble_notifications_text)
        notificationsView = view.findViewById(R.id.bubble_notifications)

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

        imageView.setImageBitmap(guildInfo.chatHeadBitmap)

        notifications = 1

        guildInfo.onAvatarChange = {
            imageView.setImageBitmap(guildInfo.chatHeadBitmap)
        }
    }

    fun clearMessages() {
        val adapter = chatHeads.content.messagesAdapter
        adapter.messages = emptyList()
        adapter.notifyDataSetChanged()
    }

    fun addMessages(msgs: List<Message>) {
        if (!baseHistoryLoaded) return

        val infos = ArrayList<MessageInfo>()

        for (message in msgs) {
            val info = MessageInfo(message)
            infos.add(info)
            messages.add(info)
        }

        Thread {
            for (info in infos) {
                val bmp = cacheAvatar(info.author)
                runOnMainLoop {
                    info.author.avatarBitmap = bmp
                }
            }
        }.start()

        if (chatHeads.activeChatHead == this) {
            val adapter = chatHeads.content.messagesAdapter
            val lm = chatHeads.content.layoutManager
            val startIndex = adapter.messages.lastIndex
            adapter.messages = messages
            adapter.notifyItemRangeInserted(startIndex, adapter.messages.lastIndex)

            if (lm.findLastVisibleItemPosition() >= startIndex - 1) {
                chatHeads.content.messagesView.smoothScrollToPosition(adapter.messages.lastIndex)
            }
        }
    }

    fun addMessage(msg: Message) {
        addMessages(listOf(msg))
    }

    override fun onSpringUpdate(spring: Spring) {
        if (spring !== this.springX && spring !== this.springY) return
        val totalVelocity = Math.hypot(springX.velocity, springY.velocity).toInt()

        chatHeads.onChatHeadSpringUpdate(this, spring, totalVelocity)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        val currentChatHead = chatHeads.chatHeads.find { it == v }!!

        val metrics = getScreenSize()

        when (event!!.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = x
                initialY = y
                initialTouchX = event.rawX
                initialTouchY = event.rawY

                scaleX = 0.92f
                scaleY = 0.92f
            }
            MotionEvent.ACTION_UP -> {
                if (!moving) {
                    if (currentChatHead == chatHeads.activeChatHead) {
                        chatHeads.collapse()
                    } else {
                        chatHeads.activeChatHead = currentChatHead
                        currentChatHead.notifications = 0
                        chatHeads.updateActiveContent()
                    }
                } else {
                    springX.endValue = metrics.widthPixels - width - chatHeads.chatHeads.indexOf(this) * (width + ChatHeads.CHAT_HEAD_EXPANDED_PADDING).toDouble()
                    springY.endValue = ChatHeads.CHAT_HEAD_EXPANDED_MARGIN_TOP.toDouble()

                    if (this == chatHeads.activeChatHead) {
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

                    if (this == chatHeads.activeChatHead) {
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