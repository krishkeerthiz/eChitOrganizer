package com.yourapp.seetuorganizer

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.yourapp.seetuorganizer.adapter.MonthCustomAdapter
import com.yourapp.seetuorganizer.adapter.SeetuCustomAdapter
import com.yourapp.seetuorganizer.adapter.UserCustomAdapter
import com.yourapp.seetuorganizer.databinding.ActivityMainBinding
import org.jsoup.Jsoup
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private var firstLaunch : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        setContentView(view)

        firstLaunch = savedInstanceState?.getBoolean("firstLaunch") ?: true

        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.setGraph(R.navigation.navigation_graph)

        NavigationUI.setupActionBarWithNavController(this, navController)

        // Update alert dialog
        if(firstLaunch) {
            GetLatestVersion().execute()
            firstLaunch = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putBoolean("firstLaunch", firstLaunch)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    override fun onSupportNavigateUp(): Boolean {
        return when(navController.currentDestination?.id){
            R.id.allUsersFragment ->{
                onBackPressed()
                true
            }
            else -> navController.navigateUp()
        }

    }

    companion object{
        lateinit var PACKAGE_NAME : String
    }

    // Update alert dialog
    fun updateAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.app_name))
        builder.setMessage(getString(R.string.update_available))
        builder.setCancelable(false)

        builder.setPositiveButton(getString(R.string.update)) { dialogInterface, _ ->
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
            dialogInterface.dismiss()
        }
        builder.setNegativeButton(getString(R.string.cancel)) { dialogInterface, _ ->
            dialogInterface.cancel()
        }

        builder.show()
    }

    //Get latest version using Jsoup
    inner class GetLatestVersion : AsyncTask<String, Unit, String>() {
        private lateinit var sLatestVersion : String

        override fun doInBackground(vararg p0: String?): String {
            try{
                sLatestVersion = Jsoup.connect(
                    "https://play.google.com/store/apps/details?id=com.yourapp.seetuorganizer"
                ).timeout(30000)
                    .get()
                    .select("div.hAyfc:nth-child(4) > span:nth-child(2) > div:nth-child(1) > span:nth-child(1)")
                    .first()
                    .ownText()
            }
            catch (e : IOException){
                sLatestVersion = ""
                //e.printStackTrace()
            }
            return sLatestVersion
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val sCurrentVersion : String = BuildConfig.VERSION_NAME

            if(sLatestVersion != ""){
                val cVersion = sCurrentVersion
                val lVersion = sLatestVersion

                if(lVersion > cVersion)
                    updateAlertDialog()
            }
        }

    }
}