package com.bdeDevTeam.runtracker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.cloudant.sync.documentstore.DocumentBodyFactory
import com.cloudant.sync.documentstore.DocumentRevision
import com.cloudant.sync.documentstore.DocumentStore
import com.cloudant.sync.replication.Replicator
import com.cloudant.sync.replication.ReplicatorBuilder
import kotlinx.android.synthetic.main.activity_launch.*
import kotlinx.android.synthetic.main.content_settings.*
import java.net.URI


class LaunchActivity : AppCompatActivity() {

    private val BACKEND_URL: String by lazy {resources.getString(R.string.backend_url)}
    private val DATABASE_NAME: String by lazy {"user_database"}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isFirstRun = getSharedPreferences("PREFERENCE", Context.MODE_PRIVATE)
                .getBoolean("isFirstRun", true)

        if (isFirstRun) {
            getSharedPreferences("PREFERENCE", Context.MODE_PRIVATE).edit().putBoolean("isFirstRun", false).apply()
            setContentView(R.layout.activity_launch)


            heightPicker.maxValue = 250
            heightPicker.minValue = 120
            heightPicker.value = 175

            agePicker.maxValue = 120
            agePicker.minValue = 6
            agePicker.value = 18

            editWeight.setOnClickListener(){
                if(editWeight.text.isEmpty()){
                    editWeight.error = "Enter your weight"
                    btnSave.isEnabled=false
                }
                else {
                    val weight: Float? = editWeight.text.toString().toFloatOrNull()
                    if (weight == null) {
                        editWeight.error = "Enter your weight"
                        btnSave.isEnabled=false
                    }
                    else if (weight < 30){
                        editWeight.error = "Minimum weight is 30 kg"
                        btnSave.isEnabled=false
                    }
                    else if (weight > 180){
                        editWeight.error= "Maximum weight is 180 kg"
                        btnSave.isEnabled=false
                    }
                    else {
                        editWeight.error = null
                        btnSave.isEnabled = true
                    }

                }
            }

            btnSave.setOnClickListener(){
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

                var databaseURI: URI = URI("$BACKEND_URL/$DATABASE_NAME")
                var uploader: Replicator = ReplicatorBuilder.push().from(store).to(databaseURI).build()
                uploader.start()

                val detailIntent: Intent = Intent(applicationContext, HomeActivity::class.java)
                detailIntent.putExtra(HomeActivity.EXTRA_DATA, dataMap)
                startActivity(detailIntent)
                finish()
            }
        }
        else {
            startActivity(Intent(baseContext, HomeActivity::class.java))
            finish()
        }

    }
}
