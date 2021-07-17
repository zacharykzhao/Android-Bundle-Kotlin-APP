package com.example.abb_demo2

import android.os.Bundle


class MainActivity : BaseSplitActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().add(
                R.id.mycontainer,
                MainFragment()
            ).commit()
        }
    }
}