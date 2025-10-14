package com.example.sailspots;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.example.sailspots.data.SpotsDao;
import com.example.sailspots.models.SpotsItem;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sailspots.databinding.ActivityMainBinding;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SpotsDao dao = new SpotsDao(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .setAnchorView(R.id.fab).show();
            }
        });
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // TEST CODE. REMOVE BEFORE DEMO
        // Adding in SpotsItem to the Database
        SpotsItem s = new SpotsItem();
        s.setName("Lake Baldwin Ramp");
        s.setType(SpotsItem.Type.RAMP);
        s.setLatitude(28.5684);
        s.setLongitude(-81.3270);
        s.setImageUrl("https://cityofwinterpark.org/wp-content/uploads/2014/05/FleetPeeplesPark_Ramp.jpg");
        s.setFavorite(false);
        dao.upsert(s);

        // Read database
        List<SpotsItem> all = dao.getAll();

        // Update favorite
        long idFetched = 1L; // Should come from the clicked item's adaptor position
        dao.setFavorite(idFetched, true);

        // Read favorites
        List<SpotsItem> favorites = dao.getFavorites();
        
        // Delete item
        dao.delete(idFetched);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}