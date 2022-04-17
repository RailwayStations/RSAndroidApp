package de.bahnhoefe.deutschlands.bahnhofsfotos;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import de.bahnhoefe.deutschlands.bahnhofsfotos.databinding.ActivityIntroSliderBinding;

public class IntroSliderActivity extends AppCompatActivity {

    private ActivityIntroSliderBinding binding;
    private int[] layouts;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final BaseApplication baseApplication = (BaseApplication) getApplication();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        binding = ActivityIntroSliderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        layouts = new int[]{R.layout.intro_slider1, R.layout.intro_slider2};

        addBottomDots(0);
        changeStatusBarColor();
        final var viewPagerAdapter = new ViewPagerAdapter();
        binding.viewPager.setAdapter(viewPagerAdapter);
        binding.viewPager.addOnPageChangeListener(viewListener);

        binding.btnSliderSkip.setOnClickListener(v -> {
            baseApplication.setFirstAppStart(true);
            openMainActivity();
        });

        binding.btnSliderNext.setOnClickListener(v -> {
            final int current = getNextItem();
            if (current < layouts.length) {
                binding.viewPager.setCurrentItem(current);
            } else {
                openMainActivity();
            }
        });
    }

    private void openMainActivity() {
        final var intent = new Intent(IntroSliderActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        openMainActivity();
    }

    private void addBottomDots(final int position) {
        final var dots = new TextView[layouts.length];
        final var colorActive = getResources().getIntArray(R.array.dot_active);
        final var colorInactive = getResources().getIntArray(R.array.dot_inactive);
        binding.layoutDots.removeAllViews();

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;", Html.FROM_HTML_MODE_LEGACY));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorInactive[position]);
            binding.layoutDots.addView(dots[i]);
        }

        if (dots.length > 0) {
            dots[position].setTextColor(colorActive[position]);
        }
    }

    private int getNextItem() {
        return binding.viewPager.getCurrentItem() + 1;
    }

    final ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(final int position) {
            final var baseApplication = (BaseApplication) getApplication();
            addBottomDots(position);

            if (position == layouts.length - 1) {
                binding.btnSliderNext.setText(R.string.proceed);
                binding.btnSliderSkip.setVisibility(View.INVISIBLE);
                baseApplication.setFirstAppStart(true);
            } else {
                binding.btnSliderNext.setText(R.string.next);
                binding.btnSliderSkip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state) {

        }
    };

    private void changeStatusBarColor() {
        final var window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    public class ViewPagerAdapter extends PagerAdapter {

        @Override
        @NonNull
        public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
            final var layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final var view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(final ViewGroup container, final int position, @NonNull final Object object) {
            final var view = (View) object;
            container.removeView(view);
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull final View view, @NonNull final Object object) {
            return view == object;
        }
    }

}
