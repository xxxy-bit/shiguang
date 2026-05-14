package com.example.quotewidget

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.DialogInterface
import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.text.TextWatcher
import android.text.Editable
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.viewpager2.widget.ViewPager2
import kotlin.concurrent.thread
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var pagerAdapter: QuotePagerAdapter
    private var quotesPage: View? = null
    private var schedulesPage: View? = null
    private var settingsPage: View? = null

    private val presetColors = listOf(
        0xFF3D3226 to "深棕", 0xFF1A1A1A to "墨黑", 0xFF8B4513 to "赭石",
        0xFF2E5D4C to "松绿", 0xFF8B2252 to "绛紫", 0xFF4A6572 to "黛蓝"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)

        pagerAdapter = QuotePagerAdapter(layoutInflater)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 2

        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = when (pos) { 0 -> "拾光"; 1 -> "日程"; else -> "设置" }
        }.attach()

        // Wait for pages to be created, then init
        viewPager.post {
            quotesPage = pagerAdapter.getPageView(0)
            schedulesPage = pagerAdapter.getPageView(1)
            settingsPage = pagerAdapter.getPageView(2)
            initQuotesPage()
            initSchedulesPage()
            if (settingsPage != null) initSettingsPage()

            // Load schedules when switching to tab 1
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    if (position == 1) loadSchedules()
                    if (position == 2 && settingsPage == null) {
                        settingsPage = pagerAdapter.getPageView(2)
                        if (settingsPage != null) initSettingsPage()
                    }
                }
            })
        }
    }

    // ====== Quote Page ======

    private fun initQuotesPage() {
        val page = quotesPage ?: return

        val input = page.findViewById<EditText>(R.id.input_text)
        val charCount = page.findViewById<TextView>(R.id.char_count)
        val statsText = page.findViewById<TextView>(R.id.stats)
        val recyclerView = page.findViewById<RecyclerView>(R.id.list_container)

        val PAGE_SIZE = 20
        var currentPage = 0
        var hasMore = true
        var isLoadingMore = false

        var adapter: QuoteAdapter? = null

        fun reloadQuotes(adp: QuoteAdapter) {
            currentPage = 0
            hasMore = true
            thread {
                val quotes = QuoteApi.fetchAll(PAGE_SIZE, 0)
                runOnUiThread {
                    adp.setQuotes(quotes)
                    if (quotes.size < PAGE_SIZE) hasMore = false
                    currentPage = 1
                    statsText.text = "已收藏 ${adp.itemCount} 条"
                }
            }
        }

        fun loadMore() {
            if (isLoadingMore || !hasMore) return
            isLoadingMore = true
            thread {
                val quotes = QuoteApi.fetchAll(PAGE_SIZE, currentPage * PAGE_SIZE)
                runOnUiThread {
                    if (quotes.isEmpty()) {
                        hasMore = false
                    } else {
                        adapter?.addQuotes(quotes)
                        currentPage++
                    }
                    adapter?.let { statsText.text = "已收藏 ${it.itemCount} 条" }
                    isLoadingMore = false
                }
            }
        }

        adapter = QuoteAdapter(
            onEdit = { id, text -> showEditDialog(id, text, adapter!!) },
            onDelete = { id ->
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("删除确认")
                    .setMessage("确定要删除这条句子吗？")
                    .setPositiveButton("删除") { _, _ ->
                        thread {
                            QuoteApi.delete(id)
                            runOnUiThread { reloadQuotes(adapter!!) }
                        }
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        )

        val adp = adapter!!

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adp

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(rv, dx, dy)
                val lm = rv.layoutManager as LinearLayoutManager
                if (lm.findLastVisibleItemPosition() >= adp.itemCount - 3) {
                    loadMore()
                }
            }
        })

        page.findViewById<Button>(R.id.btn_add).setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            thread {
                QuoteApi.add(text)
                runOnUiThread {
                    input.text.clear()
                    Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
                    reloadQuotes(adp)
                }
            }
        }

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                charCount.text = "${s.length}/100"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        reloadQuotes(adp)
    }

    private fun showEditDialog(id: Int, oldText: String, adapter: QuoteAdapter) {
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
                        runOnUiThread { adapter.updateQuote(id, newText) }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ====== Settings Page ======

    private fun initSettingsPage() {
        val page = settingsPage ?: return

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

        val savedColor = WidgetPrefs.getTextColor(this)
        for ((color, name) in presetColors) {
            val chip = layoutInflater.inflate(R.layout.color_chip, colorContainer, false)
            val dot = chip.findViewById<View>(R.id.color_dot)
            dot.setBackgroundColor(color.toInt())
            if (color == savedColor.toLong()) chip.isSelected = true
            chip.setOnClickListener {
                for (i in 0 until colorContainer.childCount) {
                    colorContainer.getChildAt(i).isSelected = false
                }
                chip.isSelected = true
                WidgetPrefs.setTextColor(this@MainActivity, color.toInt())
                updateWidget()
            }
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            colorContainer.addView(chip, params)
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

        page.findViewById<Button>(R.id.btn_apply_style).setOnClickListener {
            updateWidget()
            Toast.makeText(this, "样式已应用", Toast.LENGTH_SHORT).show()
        }

        // Auto-refresh interval
        val intervalSeek = page.findViewById<SeekBar>(R.id.interval_seek)
        val intervalLabel = page.findViewById<TextView>(R.id.interval_label)
        val intervals = listOf(0, 1, 5, 10)
        val labels = listOf("关闭", "每 1 分钟", "每 5 分钟", "每 10 分钟")
        val savedInterval = WidgetPrefs.getIntervalMinutes(this)
        val savedPos = intervals.indexOf(savedInterval).coerceAtLeast(0)
        intervalSeek.progress = savedPos
        intervalLabel.text = labels[savedPos]

        intervalSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                intervalLabel.text = labels[p.coerceIn(0, 3)]
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {
                val min = intervals[s.progress.coerceIn(0, 3)]
                WidgetPrefs.setIntervalMinutes(this@MainActivity, min)
                updateWidget()
            }
        })
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
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("删除确认")
                            .setMessage("确定要删除这个日程吗？")
                            .setPositiveButton("删除") { _, _ ->
                                thread {
                                    QuoteApi.deleteSchedule(s.id)
                                    runOnUiThread { loadSchedules() }
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
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
