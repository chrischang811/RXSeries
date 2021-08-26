package com.learn2crack.rx1000;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;

public class BaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private String bits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        bits=intent.getStringExtra("device_bits");
        setContentView(R.layout.base_navi_view);

        NavigationView navigationView = findViewById(R.id.navigation_view);

        Toolbar toolbar_all = findViewById(R.id.toolbar_all);
        setSupportActionBar(toolbar_all);
        DrawerLayout drawer_all = findViewById(R.id.drawer_layout_all);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer_all, toolbar_all, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer_all.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.nav_item_two) {
            Intent intent = new Intent(BaseActivity.this, MainActivity.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_three) {
            Intent intent = new Intent(BaseActivity.this, read_harmonic.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_four) {
            Intent intent = new Intent(BaseActivity.this, write_ftm.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_five) {
            Intent intent = new Intent(BaseActivity.this, alarm_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_six) {
            Intent intent = new Intent(BaseActivity.this, clock_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_seven) {
            Intent intent = new Intent(BaseActivity.this, comm_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_eight) {
            Intent intent = new Intent(BaseActivity.this, ratestep_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_nine) {
            Intent intent = new Intent(BaseActivity.this, alarm_record.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_ten) {
            Intent intent = new Intent(BaseActivity.this, system_record.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_eleven) {
            Intent intent = new Intent(BaseActivity.this, step_record.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_fourteen) {
            Intent intent = new Intent(BaseActivity.this, step_on_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_twelve) {
            Intent intent = new Intent(BaseActivity.this, step_on_timer.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_thirteen) {
            Intent intent = new Intent(BaseActivity.this, step_on_counter.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }

        return false;

    }

  /*@Override
   protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        actionBarDrawerToggle.syncState();
    }
*/
}