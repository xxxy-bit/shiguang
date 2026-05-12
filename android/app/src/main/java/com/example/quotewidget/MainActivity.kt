package com.example.quotewidget

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.DialogInterface
import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import kotlin.concurrent.thread
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var pagerAdapter: QuotePagerAdapter
    private var quotesPage: View? = null
    private var schedulesPage: View? = null

    private val presetColors = listOf(
        0xFF333333 to "深灰", 0xFF1A1A1A to "黑色", 0xFF1565C0 to "蓝色",
        0xFF2E7D32 to "绿色", 0xFFC62828 to "红色", 0xFF6A1B9A to "紫色"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        pagerAdapter = QuotePagerAdapter(layoutInflater)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 1

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = if (pos == 0) "拾光" else "日程"
        }.attach()

        // Wait for pages to be created, then init
        viewPager.post {
            quotesPage = pagerAdapter.getPageView(0)
            schedulesPage = pagerAdapter.getPageView(1)
            initQuotesPage()
            initSchedulesPage()

            // Load schedules when switching to tab 1
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position == 1) loadSchedules()
                }
            })
        }
    }

    // ====== Quote Page ======

    private fun initQuotesPage() {
        val page = quotesPage ?: return

        val input = page.findViewById<EditText>(R.id.input_text)
        val statsText = page.findViewById<TextView>(R.id.stats)
        val listContainer = page.findViewById<LinearLayout>(R.id.list_container)
        val fontSizeSeek = page.findViewById<SeekBar>(R.id.font_size_seek)
        val fontSizeLabel = page.findViewById<TextView>(R.id.font_size_label)
        val cornerSeek = page.findViewById<SeekBar>(R.id.corner_seek)
        val cornerLabel = page.findViewById<TextView>(R.id.corner_label)
        val colorContainer = page.findViewById<LinearLayout>(R.id.color_container)

        val savedSize = WidgetPrefs.getFontSize(this)
        fontSizeSeek.progress = ((savedSize - 14) / 10 * 100).toInt()
        fontSizeLabel.text = "字号: ${savedSize.toInt()}sp"

        val savedCorner = WidgetPrefs.getCornerRadius(this)
        cornerSeek.progress = ((savedCorner / 40) * 100).toInt()
        cornerLabel.text = "圆角: ${savedCorner.toInt()}dp"

        val maxCharsSeek = page.findViewById<SeekBar>(R.id.max_chars_seek)
        val maxCharsLabel = page.findViewById<TextView>(R.id.max_chars_label)
        val savedMaxChars = WidgetPrefs.getMaxChars(this)
        maxCharsSeek.progress = ((savedMaxChars - 20) / 380f * 100).toInt().coerceIn(0, 100)
        maxCharsLabel.text = "小组件最多显示: ${savedMaxChars}字"

        val savedColor = WidgetPrefs.getTextColor(this)
        for ((color, name) in presetColors) {
            val chip = layoutInflater.inflate(R.layout.color_chip, colorContainer, false)
            val dot = chip.findViewById<View>(R.id.color_dot)
            dot.setBackgroundColor(color.toInt())
            if (color == savedColor.toLong()) chip.isSelected = true
            chip.setOnClickListener {
                for (i in 0 until colorContainer.childCount) {
                    val child = colorContainer.getChildAt(i)
                    if (child != chip) child.isSelected = false
                }
                chip.isSelected = true
                WidgetPrefs.setTextColor(this@MainActivity, color.toInt())
                updateWidget()
            }
            // Distribute evenly
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            colorContainer.addView(chip, params)
        }

        page.findViewById<Button>(R.id.btn_add).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            thread {
                QuoteApi.add(text)
                runOnUiThread {
                    input.text.clear()
                    Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
                    loadQuotes(listContainer, statsText)
                }
            }
        }

        page.findViewById<Button>(R.id.btn_apply_style).setOnClickListener {
            updateWidget()
            Toast.makeText(this, "样式已应用", Toast.LENGTH_SHORT).show()
        }

        fontSizeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                fontSizeLabel.text = "字号: ${(14 + p / 100f * 10).toInt()}sp"
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                WidgetPrefs.setFontSize(this@MainActivity, 14f + s.progress / 100f * 10f)
                updateWidget()
            }
        })

        cornerSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                cornerLabel.text = "圆角: ${(p / 100f * 40).toInt()}dp"
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                WidgetPrefs.setCornerRadius(this@MainActivity, s.progress / 100f * 40f)
                updateWidget()
            }
        })

        maxCharsSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                val chars = (20 + p / 100f * 380).toInt()
                maxCharsLabel.text = "小组件最多显示: ${chars}字"
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                val chars = (20 + s.progress / 100f * 380).toInt()
                WidgetPrefs.setMaxChars(this@MainActivity, chars)
                updateWidget()
            }
        })

        loadQuotes(listContainer, statsText)
    }

    private fun loadQuotes(listContainer: LinearLayout, statsText: TextView) {
        thread {
            val quotes = QuoteApi.fetchAll()
            runOnUiThread {
                statsText.text = "已收藏 ${quotes.size} 条"
                listContainer.removeAllViews()
                for (q in quotes) {
                    val item = layoutInflater.inflate(R.layout.item_quote, listContainer, false)
                    item.findViewById<TextView>(R.id.quote_text).text = q.text
                    item.findViewById<TextView>(R.id.quote_meta).text = "#${q.id}  ${q.created_at}"
                    item.findViewById<ImageView>(R.id.btn_delete).setOnClickListener {
                        thread {
                            QuoteApi.delete(q.id)
                            runOnUiThread { loadQuotes(listContainer, statsText) }
                        }
                    }
                    item.findViewById<ImageView>(R.id.btn_edit).setOnClickListener {
                        showEditDialog(q.id, q.text, listContainer, statsText)
                    }
                    listContainer.addView(item)
                }
            }
        }
    }

    private fun showEditDialog(id: Int, oldText: String, listContainer: LinearLayout, statsText: TextView) {
        val input = EditText(this).apply {
            setText(oldText)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setPadding(40, 40, 40, 40)
            textSize = 15f
        }
        AlertDialog.Builder(this)
            .setTitle("编辑句子")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isNotEmpty()) {
                    thread {
                        QuoteApi.update(id, newText)
                        runOnUiThread { loadQuotes(listContainer, statsText) }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ====== Schedule Page ======

    private fun initSchedulesPage() {
        val page = schedulesPage ?: return

        val schTitle = page.findViewById<EditText>(R.id.sch_title)
        val schDate = page.findViewById<EditText>(R.id.sch_date)
        val schTime = page.findViewById<EditText>(R.id.sch_time)
        val scheduleContainer = page.findViewById<LinearLayout>(R.id.schedule_container)

        schDate.setOnClickListener { showDatePicker(schDate) }
        schTime.setOnClickListener { showTimePicker(schTime) }

        page.findViewById<Button>(R.id.btn_sch_add).setOnClickListener {
            val title = schTitle.text.toString().trim()
            if (title.isEmpty()) return@setOnClickListener
            val date = schDate.text.toString()
            val time = schTime.text.toString()
            thread {
                QuoteApi.addSchedule(title, date, time)
                runOnUiThread {
                    schTitle.text.clear()
                    schDate.text.clear()
                    schTime.text.clear()
                    Toast.makeText(this, "日程已添加", Toast.LENGTH_SHORT).show()
                    loadSchedules()
                }
            }
        }
    }

    private fun showDatePicker(target: EditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            target.setText("${y}-${"%02d".format(m + 1)}-${"%02d".format(d)}")
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(target: EditText) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, min ->
            target.setText("${"%02d".format(h)}:${"%02d".format(min)}")
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun loadSchedules() {
        val page = schedulesPage ?: return
        val scheduleContainer = page.findViewById<LinearLayout>(R.id.schedule_container)

        thread {
            val schedules = QuoteApi.fetchSchedules()
            runOnUiThread {
                scheduleContainer.removeAllViews()
                for (s in schedules) {
                    val item = layoutInflater.inflate(R.layout.item_schedule, scheduleContainer, false)
                    val cb = item.findViewById<CheckBox>(R.id.sch_done)
                    val titleView = item.findViewById<TextView>(R.id.sch_item_title)
                    val dateView = item.findViewById<TextView>(R.id.sch_item_date)
                    val delBtn = item.findViewById<ImageView>(R.id.sch_delete)

                    cb.isChecked = s.done == 1
                    titleView.text = s.title
                    if (s.done == 1) {
                        titleView.paintFlags = titleView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    }
                    dateView.text = if (s.date.isNotEmpty()) "${s.date}  ${s.time}" else "未设时间"

                    cb.setOnCheckedChangeListener { _, checked ->
                        thread {
                            QuoteApi.toggleSchedule(s.id, if (checked) 1 else 0)
                            runOnUiThread { loadSchedules() }
                        }
                    }
                    item.setOnClickListener {
                        cb.isChecked = !cb.isChecked
                    }
                    delBtn.setOnClickListener {
                        thread {
                            QuoteApi.deleteSchedule(s.id)
                            runOnUiThread { loadSchedules() }
                        }
                    }

                    scheduleContainer.addView(item)
                }
            }
        }
    }

    private fun updateWidget() {
        val manager = AppWidgetManager.getInstance(this)
        val component = ComponentName(this, QuoteWidget::class.java)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isNotEmpty()) {
            val intent = android.content.Intent(this, QuoteWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            sendBroadcast(intent)
        }
    }
}
