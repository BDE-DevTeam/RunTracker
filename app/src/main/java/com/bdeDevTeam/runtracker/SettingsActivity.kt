package com.bdeDevTeam.runtracker

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import com.cloudant.sync.documentstore.DocumentBodyFactory
import com.cloudant.sync.documentstore.DocumentRevision
import com.cloudant.sync.documentstore.DocumentStore
import com.cloudant.sync.replication.Replicator
import com.cloudant.sync.replication.ReplicatorBuilder
import kotlinx.android.synthetic.main.content_settings.*
import java.net.URI

class SettingsActivity : AppCompatActivity() {

    private val BACKEND_URL: String by lazy {resources.getString(R.string.backend_url)}
    private val USER_DATABASE: String by lazy {"user_database"}

    private lateinit var mData: UserData
    private var mUserStore: DocumentStore? = null
    companion object {
        const val EXTRA_DATA = "user_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val toolbar = findViewById<Toolbar>(R.id.action)
        setSupportActionBar(toolbar)

        var extra = intent.getSerializableExtra(EXTRA_DATA) as Map<String, Any>
        mData = UserData(extra)

        if (mData.getSex() == "Male"){
            switchBtn.isChecked = true
        }

        if (mData.getSex() == "Female"){
            switchBtn.isChecked = false
        }

        heightPicker.maxValue = 250
        heightPicker.minValue = 120
        heightPicker.value = mData.getHeight()

        agePicker.maxValue = 120
        agePicker.minValue = 6
        agePicker.value = mData.getAge()

        editWeight.setText(mData.getWeight().toString(), TextView.BufferType.EDITABLE)

        editWeight.setOnClickListener(){
            if(editWeight.text.isEmpty()){
                editWeight.error = "Enter your weight"
            }
            else {
                val weight: Float? = editWeight.text.toString().toFloatOrNull()
                if (weight == null) {
                    editWeight.error = "Enter your weight"
                }
                else if (weight < 30){
                    editWeight.error = "Minimum weight is 30 kg"
                }
                else if (weight > 180){
                    editWeight.error= "Maximum weight is 180 kg"
                }
                else {
                    editWeight.error = null
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu2, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsSave){
            var mSex = if(switchBtn.isChecked) switchBtn.textOn.toString()
            else switchBtn.textOff.toString()
            val mHeight: Int = heightPicker.value
            val mWeight: Float = editWeight.text.toString().toFloat()
            val mAge: Int = agePicker.value
            val dataMap: HashMap<String, Any> = UserData.CreateMap(mSex, mHeight, mWeight, mAge)

            var document: DocumentRevision = DocumentRevision()
            document.body = DocumentBodyFactory.create(dataMap)
            var store: DocumentStore = DocumentStore.getInstance(getDir("UserDocumentStore", Context.MODE_PRIVATE))
            store.database().create(document)
            var databaseURI: URI = URI("$BACKEND_URL/$USER_DATABASE")
            var uploader: Replicator = ReplicatorBuilder.push().from(store).to(databaseURI).build()
            uploader.start()
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}
