/*
 * MainActivity.kt
 *
 * Copyright 2020-2021 Yasuhiro Yamakawa <withlet11@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.withlet11.digitalquadrant

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class MainActivity : AppCompatActivity() {
    private val tabAdapter = TabAdapter(this, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.my_toolbar)
        toolbar.setTitle(R.string.app_name)
        toolbar.inflateMenu(R.menu.menu_main)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_licenses -> {
                    startActivity(Intent(application, LicenseActivity::class.java))
                }
                R.id.item_credits -> {
                    startActivity(Intent(this, OssLicensesMenuActivity::class.java))
                }
            }
            true
        }

        val switch: SwitchCompat = findViewById(R.id.view_switch)
        switch.setOnCheckedChangeListener { _, isChecked ->
            tabAdapter.isAutoHoldEnabled = isChecked
        }

        val pager = findViewById<ViewPager2>(R.id.pager)
        pager.adapter = tabAdapter
        // supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.autoLockCheck -> {
                item.isChecked = !item.isChecked
                tabAdapter.isAutoHoldEnabled = item.isChecked
            }
            R.id.item_licenses -> {
                startActivity(Intent(application, LicenseActivity::class.java))
            }
            R.id.item_credits -> {
                startActivity(Intent(this, OssLicensesMenuActivity::class.java))
            }
            // android.R.id.home -> finish()
        }

        return true
    }
     */
}
