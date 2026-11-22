package com.kiyomizusuzu.shizu

import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.R
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.BreakIterator
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object Colors {
    val BACKGROUND = Color.rgb(16, 19, 29)
    val BOX = Color.rgb(28, 35, 51)
    val BUTTON = Color.rgb(145, 169, 255)
    val RESULT = Color.rgb(80, 56, 137)
    val TEXT = Color.rgb(231, 226, 237)
    val DETAIL = Color.rgb(195, 191, 206)
    val LINE = Color.rgb(65, 73, 92)
    val STAGE = Color.rgb(75, 83, 105)
    val LABEL = Color.rgb(35, 35, 65)
}
private enum class LiveCheck(val color: Int) {
    CORRECT(Color.rgb(85, 200, 120)),
    ALMOST(Color.rgb(255, 181, 71)),
    WRONG(Color.rgb(240, 82, 82))
}
private data class Exam(
    val sentence: String,
    val seconds: Double,
    val cpm: Double,
    val accuracy: Double,
    val score: Int
)
private data class Session(
    val timestamp: Long,
    val tests: List<Exam>
)
class MainActivity : ComponentActivity() {
    // data to practise on
    private val answers = listOf(
        "きょうはきゃくがきます。",
        "わたしはにほんごをべんきょうします。",
        "おはようございます。",
        "わたしはがくせいです。",
        "きょうはいいてんきです。",
        "ねこがすきです。",
        "みずをのみます。",
        "あしたはがっこうへいきます。",
        "ともだちとこうえんへいきます。",
        "まいにちにほんごをれんしゅうします。"
    )
    // documents
    private val results = mutableListOf<Exam>()
    private val practise = mutableListOf<Session>()
    private val currentSessionResults = mutableListOf<Exam>()
    // default values
    private var currentTest = 0
    private var eachCharacter = emptyList<String>()
    private var lastTyped = emptyList<String>()
    private var previousStatus = emptyList<LiveCheck>()
    private var score = 0
    private var startedAt = 0L
    private var testFinished = false
    private var clearInputBox = false
    // setting up for android views
    private lateinit var homeScreen: View
    private lateinit var sentenceVisible: TextView
    private lateinit var inputPreview: EditText
    private lateinit var progressCount: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var avgSpeedView: TextView
    private lateinit var avgAccuracyView: TextView
    // Access your private storage and keyboard
    private val storehistory by lazy { 
        getSharedPreferences(PREFERENCESFILE, MODE_PRIVATE) 
    }
    private val keyboardManager get() = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    // EXECUTION
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // Configured to set dark screen when booting up the app
        super.onCreate(savedInstanceState)
        actionBar?.hide() // Removes the annoying header at the top
        loadHistory()
        buildHomeScreen()
        setupBackNavigation()
        startTest()
    }
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    homeScreen.parent == null && !testFinished -> {
                        setContentView(homeScreen)
                        inputPreview.clearFocus()
                        hideKeyboard()
                    }
                    testFinished -> {
                        currentTest = 0
                        currentSessionResults.clear()
                        buildHomeScreen()
                        startTest()
                        inputPreview.clearFocus()
                        hideKeyboard()
                    }
                    else -> {
                        finish()
                    }
                }
            }
        })
    }
    private fun startTest() {
        testFinished = false
        startedAt = 0L
        score = 0
        eachCharacter = splitIntoCharacters(answers[currentTest])
        lastTyped = emptyList()
        previousStatus = emptyList()
        clearInputBox = true
        inputPreview.setText("")
        clearInputBox = false
        showSentence()
        refreshOverview()
    }
    private fun typing(text: Editable) {
        val typed = splitIntoCharacters(text.toString())
        val converting = BaseInputConnection.getComposingSpanStart(text) >= 0
        if (typed.size >= eachCharacter.size && !converting) {
            finishTest()
        }
        else {
            if (typed.isNotEmpty() && startedAt == 0L) {
                startedAt = SystemClock.elapsedRealtime()
            }
            val statuses = typed.mapIndexed { i, ch ->
                eachCharacter.getOrNull(i)?.let { classify(ch, it) } ?: LiveCheck.WRONG
            }
            val unchanged = lastTyped.zip(typed).takeWhile { [old, new] -> old == new }.size
            for (i in unchanged until typed.size) {
                when (statuses[i]) {
                    LiveCheck.CORRECT -> score++
                    LiveCheck.WRONG -> score--
                    LiveCheck.ALMOST -> {}
                }
            }
            lastTyped = typed
            previousStatus = statuses
            showSentence()
        }
    }
    private fun finishTest() {
        if (testFinished) {
            return
        }
        else {
            testFinished = true
            val seconds =   if (startedAt == 0L) {
                                0.0 
                            }
                            else {
                                (SystemClock.elapsedRealtime() - startedAt) / 1000.0
                            }
            val experienced = previousStatus.count { 
                it == LiveCheck.CORRECT 
            }
            val result = Exam(sentence = answers[currentTest],
                            seconds = seconds,
                            cpm = if (seconds > 0.0) {
                                        experienced / (seconds / 60.0) 
                                    }
                                    else {
                                        0.0
                                    },
                            accuracy = (score.toDouble() / eachCharacter.size * 100.0).coerceIn(0.0, 100.0),
                            score = score
                    )
            results += result
            currentSessionResults += result
            saveResults()
            refreshOverview()
            if (currentTest == answers.lastIndex) {
                practise += Session(System.currentTimeMillis(), currentSessionResults.toList())
                saveSessions()
                inputPreview.post {
                    showResults()
                }
            } 
            else {
                inputPreview.post {
                    currentTest++
                    startTest()
                    showKeyboard()
                }
            }     
        }
    }
    private fun showSentence() {
        val styled = SpannableStringBuilder()
        eachCharacter.forEachIndexed { i, ch ->
            val start = styled.length
            styled.append(ch)
            val color = previousStatus.getOrNull(i)?.color ?: Colors.DETAIL
            styled.setSpan(ForegroundColorSpan(color), start, styled.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        sentenceVisible.text = styled
    }
    private fun refreshOverview() {
        progressCount.text = "${currentSessionResults.size} / ${answers.size}"
        progressBar.progress = currentSessionResults.size
        avgSpeedView.text = "TOTAL SPEED\n${results.overallCpm().formatDecimal(0)} CPM"
        avgAccuracyView.text = "TOTAL ACCURACY\n${results.mean { it.accuracy }.formatDecimal(1)}%"
    }
    private fun showKeyboard() {
        inputPreview.post {
            inputPreview.requestFocus()
            keyboardManager.showSoftInput(inputPreview, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    private fun hideKeyboard() {
        keyboardManager.hideSoftInputFromWindow(inputPreview.windowToken, 0)
    }
    private fun buildHomeScreen() {
        progressCount = highlight("", 17f, Colors.BUTTON, bold = true)
        progressBar = ProgressBar(this, null, R.attr.progressBarStyleHorizontal).apply {
            max = answers.size
            progressTintList = ColorStateList.valueOf(Colors.BUTTON)
            progressBackgroundTintList = ColorStateList.valueOf(Colors.STAGE)
        }
        avgSpeedView = statBox()
        avgAccuracyView = statBox()
        sentenceVisible = highlight("", 34f, center = true).apply {
            typeface = Typeface.SANS_SERIF
            padDp(top = 14, bottom = 10)
        }
        inputPreview = EditText(this).apply {
            textSize = sp(18f)
            maxLines = 2
            gravity = Gravity.CENTER_VERTICAL
            hint = "Your answer"
            setTextColor(Colors.TEXT)
            setHintTextColor(Colors.DETAIL)
            padDp(14, 8, 14, 8)
            background = rounded(Colors.BACKGROUND, 12, Colors.BOX)
        }
        inputPreview.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Nothing needed before the text changes.
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Nothing needed while the text is changing.
            }
            override fun afterTextChanged(s: Editable?) {
                if (s != null && !clearInputBox && !testFinished) typing(s)
            }
        })
        val correctOrNot = listOf(
            LiveCheck.CORRECT.color to "Correct",
            LiveCheck.ALMOST.color to "Almost correct",
            LiveCheck.WRONG.color to "Wrong",
            Colors.DETAIL to "Not typed yet"
        )
        homeScreen = screen(sidePadding = 12) {
            addView(highlight("かな TYPING", 32f, Colors.BUTTON, bold = true, center = true).apply {
                padDp(top = 28, bottom = 16)
            })
            addView(row {
                addView(highlight("YOUR PROGRESS", 14f, Colors.DETAIL, bold = true), lp(width = 0, weight = 1f))
                addView(progressCount)
            })
            addView(progressBar, lp(height = dp(8), top = 14, bottom = 28))
            addView(row {
                gravity = Gravity.CENTER
                for (stat in listOf(avgSpeedView, avgAccuracyView)) {
                    addView(stat, lp(width = 0, height = dp(100), weight = 1f).apply {
                        marginStart = dp(5)
                        marginEnd = dp(5)
                    })
                }
            })
            addView(box {
                gravity = Gravity.CENTER
                padDp(16, 16, 16, 16)
                addView(highlight("TYPE THIS SENTENCE", 13f, Colors.DETAIL, bold = true, center = true).apply {
                    letterSpacing = 0.08f
                })
                addView(sentenceVisible)
            }, lp(top = 10))
            addView(inputPreview, lp(top = 10))
            addView(box {
                padDp(22, 18, 22, 18)
                for ([color, name] in correctOrNot) {
                    addView(row {
                        padDp(top = 8, bottom = 8)
                        addView(highlight("●", 17f, color))
                        addView(highlight(name, 15f, Colors.DETAIL).apply { 
                            padDp(left = 12) 
                        })
                    })
                }
            }, lp(top = 24))
            addView(button("View History") { 
                showHistory() 
            }, lp(top = 18, bottom = 20))
        }
        setContentView(homeScreen)
    }
    private fun showResults() {
        val tests = currentSessionResults.toList()
        setContentView(screen(sidePadding = 20) {
            addView(highlight("🎉\nTest Completed!", 28f, bold = true, center = true).apply {
                padDp(top = 28, bottom = 6)
            })
            addView(highlight("You completed all ${answers.size} answers.", 15f, Colors.DETAIL, center = true).apply {
                padDp(bottom = 24)
            })
            addView(vertical {
                gravity = Gravity.CENTER
                padDp(22, 28, 22, 28)
                background = rounded(Colors.RESULT, 26)
                addView(highlight("FINAL TYPING SPEED", 17f, bold = true, center = true))
                addView(highlight(tests.overallCpm().formatDecimal(0), 62f, bold = true, center = true).apply {
                    padDp(top = 12)
                })
                addView(highlight("Characters per minute", 17f, center = true))
                val accuracy =  tests.mean { 
                                    it.accuracy 
                                }.formatDecimal(1)
                addView(highlight("Average accuracy: $accuracy%", 16f, bold = true, center = true).apply {
                    padDp(top = 18)
                })
            }, lp(top = 28))
            addView(highlight("Individual tests", 21f, bold = true).apply { 
                padDp(top = 28, bottom = 14) 
            })
            tests.forEachIndexed { i, test ->
                addView(box {
                    addView(highlight("TEST ${i + 1}", 12f, Colors.BUTTON, bold = true).apply { 
                        letterSpacing = 0.12f 
                    })
                    addView(highlight(test.sentence, 19f).apply { 
                        padDp(top = 12, bottom = 14) 
                    })
                    addMetrics(test)
                }, lp(bottom = 14))
            }
            addView(button("Back to Home") {
                currentTest = 0
                currentSessionResults.clear()
                buildHomeScreen()
                startTest()
                inputPreview.clearFocus()
                hideKeyboard()
            }, lp(top = 18, bottom = 20))
        })
    }
    private fun showHistory() {
        val dateFormat = SimpleDateFormat("MMM d, yyyy  •  h:mm a", Locale.getDefault())
        val newestFirst = practise.sortedByDescending { it.timestamp }
        setContentView(screen(sidePadding = 20) {
            addView(highlight("Your History", 28f, bold = true, center = true).apply { 
                padDp(top = 28, bottom = 24) 
            })
            if (newestFirst.isEmpty()) {
                val message = "No completed sessions yet.\n\nYour completed practice sessions will appear here."
                addView(highlight(message, 16f, Colors.DETAIL, center = true).apply { 
                    padDp(20, 40, 20, 40) 
                })
            }
            else {
                newestFirst.forEachIndexed { i, session ->
                    addView(sessionBox(newestFirst.size - i, session, dateFormat), lp(bottom = 12))
                }
            }
            addView(button("Close History") {
                setContentView(homeScreen)
                inputPreview.clearFocus()
                hideKeyboard()
            }, lp(top = 12, bottom = 20))
        })
        window.decorView.post {
            hideKeyboard()
        }
    }
    private fun sessionBox(number: Int, session: Session, dateFormat: SimpleDateFormat): LinearLayout {
        val tests = session.tests
        val expand = highlight("▼", 18f, Colors.BUTTON, bold = true, center = true)
        val details = vertical {
            visibility = View.GONE
            padDp(top = 18)
        }
        val titles = vertical {
            addView(highlight("SESSION $number", 15f, Colors.BUTTON, bold = true))
            val date = dateFormat.format(Date(session.timestamp))
            addView(highlight(date, 13f, Colors.DETAIL).apply {
                padDp(top = 6, bottom = 8)
            })
            val summary = "${tests.size} tests  •  ${tests.overallCpm().formatDecimal(0)} CPM  •  " + "${tests.mean { it.accuracy }.formatDecimal(1)}% accuracy"
            addView(highlight(summary, 13f))
        }
        val header = row {
            isClickable = true
            isFocusable = true
            padDp(top = 4, bottom = 4)
            addView(titles, lp(width = 0, weight = 1f))
            addView(expand, lp(width = dp(36), height = MATCH_PARENT))
            setOnClickListener {
                val expanded = details.visibility == View.VISIBLE
                if (!expanded && details.childCount == 0) {
                    tests.forEachIndexed { i, test ->
                        details.addView(highlight("TEST ${i + 1}", 12f, Colors.BUTTON, bold = true).apply {
                            padDp(top = 10, bottom = 8)
                        })
                        details.addView(highlight(test.sentence, 17f).apply {
                            padDp(bottom = 8)
                        })
                        details.addMetrics(test)
                        if (i < tests.lastIndex) {
                            val divider = View(this@MainActivity).apply {
                                setBackgroundColor(Colors.LINE)
                            }
                            details.addView(divider,lp(height = dp(1), top = 12, bottom = 8))
                        }
                    }
                }
                details.visibility = if (expanded) View.GONE else View.VISIBLE
                expand.text = if (expanded) "▼" else "▲"
            }
        }
        return box {
            addView(header)
            addView(details)
        }
    }
    private fun saveResults() {
        val json = JSONArray(results.map { 
            it.toJson() 
        })
        storehistory.edit().putString(KEY_TESTS, json.toString()).apply()
    }
    private fun saveSessions() {
        val json = JSONArray(practise.map { (timestamp, tests) ->
            JSONObject().put("timestamp", timestamp).put("tests", JSONArray(tests.map { 
                it.toJson() 
            }))
        })
        storehistory.edit().putString(KEY_SESSIONS, json.toString()).apply()
    }
    private fun loadHistory() {
        try {
            storehistory.getString(KEY_TESTS, null)?.let { 
                results += JSONArray(it).toFormats() 
            }
        } 
        catch (error: JSONException) {
            results.clear() //clears the results list instead of crashing if json is corrupted for some unknown reasons.
        }
        val savedSessions = storehistory.getString(KEY_SESSIONS, null)
        if (savedSessions != null) {
            try {
                val array = JSONArray(savedSessions)
                for (i in 0 until array.length()) {
                    val session = array.getJSONObject(i)
                    practise += Session(
                        timestamp = session.optLong("timestamp", 0L),
                        tests = session.getJSONArray("tests").toFormats()
                    )
                }
            } 
            catch (error: JSONException) {
                practise.clear() //clears the session history instead of crashing if again json is corrupted
            }
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density * UI_SCALE).toInt()
    private fun sp(value: Float) = value * TEXT_SCALE
    private fun View.padDp(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) = setPadding(dp(left), dp(top), dp(right), dp(bottom))
    private fun lp(
        width: Int = MATCH_PARENT,
        height: Int = WRAP_CONTENT,
        weight: Float = 0f,
        top: Int = 0,
        bottom: Int = 0
    ) = LinearLayout.LayoutParams(width, height, weight).apply {
        topMargin = dp(top)
        bottomMargin = dp(bottom)
    }
    private fun rounded(color: Int, radiusDp: Int, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null) {
            setStroke(dp(1), stroke)
        }
    }
    private fun highlight(
        value: String,
        size: Float,
        color: Int = Colors.TEXT,
        bold: Boolean = false,
        center: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = sp(size)
        setTextColor(color)
        if (bold) {
            typeface = Typeface.DEFAULT_BOLD
        }
        if (center) {
            gravity = Gravity.CENTER
        }
    }
    private fun button(caption: String, onClick: () -> Unit) = Button(this).apply {
        text = caption
        textSize = sp(16f)
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(Colors.LABEL)
        backgroundTintList = ColorStateList.valueOf(Colors.BUTTON)
        setOnClickListener { 
            onClick() 
        }
    }
    private fun vertical(build: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        build()
    }
    private fun row(build: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        build()
    }
    private fun box(build: LinearLayout.() -> Unit) = vertical {
        padDp(20, 20, 20, 20)
        background = rounded(Colors.BOX, 22)
        build()
    }
    private fun statBox() = highlight("", 14f, bold = true, center = true).apply {
        background = rounded(Colors.BOX, 20)
    }
    private fun screen(sidePadding: Int, build: LinearLayout.() -> Unit): LinearLayout {
        val scroll = ScrollView(this).apply { 
            addView(vertical(build)) 
        }
        return vertical {
            setBackgroundColor(Colors.BACKGROUND)
            padDp(sidePadding, 24, sidePadding, 20)
            addView(scroll, lp(height = 0, weight = 1f))
        }
    }
    private fun LinearLayout.addMetrics(test: Exam) {
        addMetric("Time", "${test.seconds.formatDecimal(1)} seconds")
        addMetric("CPM", test.cpm.formatDecimal(1))
        addMetric("Accuracy", "${test.accuracy.formatDecimal(1)}%")
    }
    private fun LinearLayout.addMetric(name: String, value: String) {
        addView(row {
            padDp(top = 7, bottom = 7)
            addView(highlight(name, 14f, Colors.DETAIL), lp(width = 0, weight = 1f))
            addView(highlight(value, 15f, bold = true).apply { 
                gravity = Gravity.END 
            })
        })
    }
    private companion object {
        const val PREFERENCESFILE = "typing_history"
        const val KEY_TESTS = "tests"
        const val KEY_SESSIONS = "sessions"
        const val UI_SCALE = 0.8f
        const val TEXT_SCALE = 0.9f
    }
}
// incase for dakuten and handakuten
private val combiningMarks = Regex("\\p{M}")
// pairs small hiragana with their regular-sized equivalents
private val smallKana = mapOf(
    'ぁ' to 'あ', 'ぃ' to 'い', 'ぅ' to 'う', 'ぇ' to 'え', 'ぉ' to 'お',
    'ゃ' to 'や', 'ゅ' to 'ゆ', 'ょ' to 'よ',
    'っ' to 'つ', 'ゎ' to 'わ', 'ゕ' to 'か', 'ゖ' to 'け'
)
private fun classify(typed: String, expected: String) = when {
    typed == expected -> LiveCheck.CORRECT
    baseKana(typed) == baseKana(expected) -> LiveCheck.ALMOST
    else -> LiveCheck.WRONG
}
private fun baseKana(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD).replace(combiningMarks, "").map { 
        smallKana[it] ?: it 
    }.joinToString("")
}
private fun splitIntoCharacters(value: String): List<String> {
    if (value.isEmpty()) {
        return emptyList()
    }
    else {
        val breaks = BreakIterator.getCharacterInstance(Locale.JAPANESE).apply { 
            setText(value) 
        }
        val chars = mutableListOf<String>()
        var start = breaks.first()
        var end = breaks.next()
        while (end != BreakIterator.DONE) {
            chars += value.substring(start, end)
            start = end
            end = breaks.next()
        }
        return chars
    }
}
private fun List<Exam>.mean(pick: (Exam) -> Double): Double {
    return if (isEmpty()) {
        0.0
    }
    else {
        map(pick).average()
    }
}
private fun List<Exam>.overallCpm(): Double {
    val totalSeconds =  sumOf { 
                            it.seconds 
                        }
    if (totalSeconds <= 0.0) {
        return 0.0
    }
    else {
        val totalCorrectCharacters =    sumOf { 
                                            (val seconds, val cpm) -> cpm * seconds / 60.0
                                        }
        return totalCorrectCharacters / totalSeconds * 60.0
    }
}
private fun Double.formatDecimal(decimals: Int): String {
    return "%.${decimals}f".format(Locale.US, this)
}
private fun Exam.toJson(): JSONObject {
    return JSONObject()
        .put("sentence", sentence)
        .put("seconds", seconds)
        .put("cpm", cpm)
        .put("accuracy", accuracy)
        .put("score", score)
}
private fun JSONObject.toFormat() = Exam(
    sentence = getString("sentence"),
    seconds = getDouble("seconds"),
    cpm = getDouble("cpm"),
    accuracy = getDouble("accuracy"),
    score = optInt("score", 0)
)
private fun JSONArray.toFormats() = (0 until length()).map { 
    getJSONObject(it).toFormat() 
}