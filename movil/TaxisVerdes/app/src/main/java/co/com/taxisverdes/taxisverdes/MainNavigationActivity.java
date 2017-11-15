package co.com.taxisverdes.taxisverdes;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import co.com.taxisverdes.taxisverdes.home.HomeFragment;
import co.com.taxisverdes.taxisverdes.utils.NotificationUtils;

public class MainNavigationActivity extends AppCompatActivity {

    private static final String TAG = MainNavigationActivity.class.getName();
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private NavigationView navigationView;

    private int lastMenuSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigation);

        this.toolbar = findViewById(R.id.toolbar);
        this.drawerLayout = findViewById(R.id.homeDrawerLayout);
        this.navigationView = findViewById(R.id.homeDrawer);
        this.actionBarDrawerToggle = setupDrawerToggle();
        this.actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
        this.drawerLayout.addDrawerListener(actionBarDrawerToggle);
        setSupportActionBar(this.toolbar);
        setupDrawerContent(this.navigationView);
        showFragment(HomeFragment.class);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        //SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        //       .findFragmentById(R.id.map);
        //mapFragment.getMapAsync(this);
    }

    private ActionBarDrawerToggle setupDrawerToggle() {
        return new ActionBarDrawerToggle(this, this.drawerLayout, this.toolbar,
                R.string.open_drawer, R.string.close_drawer);
    }

    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectDrawerItem(item);
                return true;
            }
        });
    }

    private void selectDrawerItem(MenuItem item) {
        Fragment fragment;
        Class fragmentClass = HomeFragment.class;

        this.lastMenuSelected = item.getItemId();
        switch (this.lastMenuSelected) {
            case R.id.menuHome:
                fragmentClass = HomeFragment.class;
                break;
            case R.id.menuMediosDePago:
                break;
            default:
                return;
        }

        item.setChecked(true);
        setTitle(item.getTitle());
        showFragment(fragmentClass);
    }

    private void showFragment(Class fragmentClass) {
        try {
            Fragment fragment = (Fragment) fragmentClass.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainNavigationContent, fragment)
                    .commit();
            this.drawerLayout.closeDrawers();
        } catch (Exception e) {
            Log.e(TAG, "Error creando fragmento", e);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
            this.actionBarDrawerToggle.syncState();
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (this.actionBarDrawerToggle.onOptionsItemSelected(item)) {
                return true;
            } else {
                int id = item.getItemId();


            }
        } catch (Exception e) {
            NotificationUtils.showGeneralError(e);
        }
        return super.onOptionsItemSelected(item);
    }
}
