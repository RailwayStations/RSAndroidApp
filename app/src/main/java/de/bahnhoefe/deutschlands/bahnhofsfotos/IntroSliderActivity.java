package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IntroSliderActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private int[] layouts;
    private LinearLayout dotsLayout;
    private Button next, skip;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final BaseApplication baseApplication = (BaseApplication) getApplication();

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_intro_slider);

        viewPager = findViewById(R.id.view_pager);
        dotsLayout = findViewById(R.id.layoutDots);
        skip = findViewById(R.id.btn_slider_skip);
        next = findViewById(R.id.btn_slider_next);

        layouts = new int[]{R.layout.intro_slider1, R.layout.intro_slider2};

        addBottomDots(0);
        changeStatusBarColor();
        final ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter();
        viewPager.setAdapter(viewPagerAdapter);
        viewPager.addOnPageChangeListener(viewListener);

        skip.setOnClickListener(v -> {
            baseApplication.setFirstAppStart(true);
            final Intent intent = new Intent(IntroSliderActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        next.setOnClickListener(v -> {
            final int current = getItem(+1);
            if (current < layouts.length) {
                viewPager.setCurrentItem(current);
            } else {
                final Intent intent = new Intent(IntroSliderActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void addBottomDots(final int position) {
        final TextView[] dots = new TextView[layouts.length];
        final int[] colorActive = getResources().getIntArray(R.array.dot_active);
        final int[] colorInactive = getResources().getIntArray(R.array.dot_inactive);
        dotsLayout.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorInactive[position]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[position].setTextColor(colorActive[position]);
        }
    }

    private int getItem(final int i) {
        return viewPager.getCurrentItem() + i;
    }

    ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(final int position) {
            final BaseApplication baseApplication = (BaseApplication) getApplication();
            addBottomDots(position);

            if (position == layouts.length - 1) {
                next.setText(R.string.proceed);
                skip.setVisibility(View.GONE);
                baseApplication.setFirstAppStart(true);
            } else {
                next.setText(R.string.next);
                skip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {

        }
    };

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public class ViewPagerAdapter extends PagerAdapter {

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            final LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            final View view = (View) object;
            container.removeView(view);
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view == object;
        }
    }

}
