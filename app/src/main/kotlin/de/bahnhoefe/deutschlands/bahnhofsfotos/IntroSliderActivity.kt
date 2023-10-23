package de.bahnhoefe.deutschlands.bahnhofsfotos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityIntroSliderBinding

class IntroSliderActivity : AppCompatActivity() {
    private var binding: ActivityIntroSliderBinding? = null
    private var layouts: IntArray
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val baseApplication = application as BaseApplication
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        binding = ActivityIntroSliderBinding.inflate(
            layoutInflater
        )
        setContentView(binding!!.root)
        layouts = intArrayOf(R.layout.intro_slider1, R.layout.intro_slider2)
        addBottomDots(0)
        changeStatusBarColor()
        val viewPagerAdapter = ViewPagerAdapter()
        binding!!.viewPager.adapter = viewPagerAdapter
        binding!!.viewPager.addOnPageChangeListener(viewListener)
        binding!!.btnSliderSkip.setOnClickListener { v: View? ->
            baseApplication.firstAppStart = true
            openMainActivity()
        }
        binding!!.btnSliderNext.setOnClickListener { v: View? ->
            val current = nextItem
            if (current < layouts.size) {
                binding!!.viewPager.currentItem = current
            } else {
                openMainActivity()
            }
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this@IntroSliderActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        openMainActivity()
    }

    private fun addBottomDots(position: Int) {
        val dots = arrayOfNulls<TextView>(layouts.size)
        val colorActive = resources.getIntArray(R.array.dot_active)
        val colorInactive = resources.getIntArray(R.array.dot_inactive)
        binding!!.layoutDots.removeAllViews()
        for (i in dots.indices) {
            dots[i] = TextView(this)
            dots[i]!!.text = Html.fromHtml("&#8226;", Html.FROM_HTML_MODE_LEGACY)
            dots[i]!!.textSize = 35f
            dots[i]!!.setTextColor(colorInactive[position])
            binding!!.layoutDots.addView(dots[i])
        }
        if (dots.size > 0) {
            dots[position]!!.setTextColor(colorActive[position])
        }
    }

    private val nextItem: Int
        private get() = binding!!.viewPager.currentItem + 1
    val viewListener: OnPageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            val baseApplication = application as BaseApplication
            addBottomDots(position)
            if (position == layouts.size - 1) {
                binding!!.btnSliderNext.setText(R.string.proceed)
                binding!!.btnSliderSkip.visibility = View.INVISIBLE
                baseApplication.firstAppStart = true
            } else {
                binding!!.btnSliderNext.setText(R.string.next)
                binding!!.btnSliderSkip.visibility = View.VISIBLE
            }
        }

        override fun onPageScrollStateChanged(state: Int) {}
    }

    private fun changeStatusBarColor() {
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
    }

    inner class ViewPagerAdapter : PagerAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val layoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val view = layoutInflater.inflate(layouts[position], container, false)
            container.addView(view)
            return view
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            val view = `object` as View
            container.removeView(view)
        }

        override fun getCount(): Int {
            return layouts.size
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }
    }
}