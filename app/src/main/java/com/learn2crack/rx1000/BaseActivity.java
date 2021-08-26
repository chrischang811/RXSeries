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
        toolbar_all.setNavigationIcon(R.mipmap.ic_launcher);

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

        if (id == R.id.nav_item_one) {
            Intent intent = new Intent(BaseActivity.this, MainActivity.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_two) {
            Intent intent = new Intent(BaseActivity.this, overcurrent.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_three) {
            Intent intent = new Intent(BaseActivity.this, clock_setting.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        }else if (id == R.id.nav_item_four) {
            Intent intent = new Intent(BaseActivity.this, earthfault.class);
            intent.putExtra("device_bits",bits);
            startActivity(intent);
        } else if (id == R.id.nav_item_five) {
            Intent intent = new Intent(BaseActivity.this, thermal_overload.class);
            intent.putExtra("device_bits", bits);
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