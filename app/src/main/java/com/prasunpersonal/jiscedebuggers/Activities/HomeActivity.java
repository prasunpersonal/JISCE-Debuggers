package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.Adapters.FragmentAdapter;
import com.prasunpersonal.jiscedebuggers.Fragments.NotificationFragment;
import com.prasunpersonal.jiscedebuggers.Fragments.OptionsFragment;
import com.prasunpersonal.jiscedebuggers.Fragments.PostsFragment;
import com.prasunpersonal.jiscedebuggers.Fragments.ProfileFragment;
import com.prasunpersonal.jiscedebuggers.Fragments.UsersFragment;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {
    ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.mainToolbar.setTitle(getString(R.string.app_name));
        binding.mainToolbar.setSubtitle(ME.getName());
        setSupportActionBar(binding.mainToolbar);

        ArrayList<Fragment> mainFragments = new ArrayList<>();
        mainFragments.add(new PostsFragment());
        mainFragments.add(new UsersFragment());
        mainFragments.add(new ProfileFragment());
        mainFragments.add(new NotificationFragment());
        mainFragments.add(new OptionsFragment());

        binding.mainViewPager.setAdapter(new FragmentAdapter(getSupportFragmentManager(), getLifecycle(), mainFragments));
        binding.mainViewPager.setOffscreenPageLimit(mainFragments.size());
        binding.mainTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.mainViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        binding.mainViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.mainTabLayout.selectTab(binding.mainTabLayout.getTabAt(position));
                super.onPageSelected(position);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.addPost) {
            startActivity(new Intent(this, CreatePostActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }
}