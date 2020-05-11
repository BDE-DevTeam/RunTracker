package com.bdeDevTeam.runtracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.cloudant.sync.documentstore.DocumentRevision
import com.cloudant.sync.documentstore.DocumentStore
import com.cloudant.sync.event.Subscribe
import com.cloudant.sync.event.notifications.ReplicationCompleted
import com.cloudant.sync.event.notifications.ReplicationErrored
import com.cloudant.sync.replication.Replicator
import com.cloudant.sync.replication.ReplicatorBuilder
import kotlinx.android.synthetic.main.activity_home.*
import java.net.URI
import kotlin.math.roundToInt

class HomeActivity : AppCompatActivity() {
    private val BACKEND_URL: String by lazy {resources.getString(R.string.backend_url)}
    private val DATABASE_NAME: String by lazy {resources.getString(R.string.main_database_name)}
    private val USER_DATABASE: String by lazy {"user_database"}

    private var mStore: DocumentStore? = null
    private var mUserStore: DocumentStore? = null
    private var mDownloader: Replicator? = null
    private var mUserDownloader: Replicator? = null

    private var mHistory: ArrayList<RunData> = ArrayList<RunData>()
    private lateinit var mData: UserData
    companion object {
        const val EXTRA_DATA = "user_data"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        var recycler: RecyclerView = findViewById(R.id.recycler_history)
        recycler.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recycler.setHasFixedSize(true)

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = HistoryAdapter(mHistory, object : HistoryAdapter.OnItemClickListener{
            override fun onItemClick(data: RunData) {
                var dataMap: HashMap<String, Any> = RunData.CreateMap(data.getDateInMillis(), data.getDurationInMillis(),
                        data.getDistance(), data.getBurntCalories(), data.getStartCoordinate(), data.getFinishCoordinate())

                var detailIntent: Intent = Intent(applicationContext, DetailActivity::class.java)
                detailIntent.putExtra(DetailActivity.EXTRA_DATA, dataMap)

                startActivity(detailIntent)
            }
        })

        fab.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startActivity(Intent(this, TrackActivity::class.java))
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            }
        }

        mStore = DocumentStore.getInstance(getDir("DocumentStore", Context.MODE_PRIVATE))

        var databaseURI: URI = URI("$BACKEND_URL/$DATABASE_NAME")
        mDownloader = ReplicatorBuilder.pull().from(databaseURI).to(mStore).build()
        mDownloader!!.eventBus.register(this)
        mDownloader!!.start()

        mUserStore = DocumentStore.getInstance(getDir("UserDocumentStore", Context.MODE_PRIVATE))
        var userDatabaseURI: URI = URI("$BACKEND_URL/$USER_DATABASE")
        mUserDownloader = ReplicatorBuilder.pull().from(userDatabaseURI).to(mUserStore).build()
        mUserDownloader!!.eventBus.register(this)
        mUserDownloader!!.start()

        readHistoryData()

    }

    override fun onStart() {
        super.onStart()

        readHistoryData()
    }

    @Subscribe
    public fun onComplete(event: ReplicationCompleted) {
        mDownloader!!.eventBus.unregister(this)
        mDownloader = null
        mUserDownloader!!.eventBus.unregister(this)
        mUserDownloader = null

        readHistoryData()
    }

    @Subscribe
    public fun onError(event: ReplicationErrored) {
        mDownloader!!.eventBus.unregister(this)
        mDownloader = null
        mUserDownloader!!.eventBus.unregister(this)
        mUserDownloader = null
    }

    private fun readHistoryData() {
        if (mStore != null) {
            var database = mStore!!.database();

            if (mHistory.count() != database.documentCount) {
                mHistory.clear()

                var documentList: List<DocumentRevision> = database.read(0, database.documentCount, true)
                for (document in documentList) {
                    mHistory.add(RunData(document.body.asMap()))
                }

                var recycler: RecyclerView = findViewById(R.id.recycler_history)
                recycler.adapter.notifyDataSetChanged()
                recycler.recycledViewPool.clear()
                recycler.invalidate()
            }
        }
        var daily:Int = -1
        if (intent.hasExtra(EXTRA_DATA)) {
            var extra = intent.getSerializableExtra(EXTRA_DATA) as Map<String, Any>
            mData = UserData(extra)
            daily = calcUserDaily(mData)

        }
        else if (mUserStore != null) {
            var database = mUserStore!!.database();
            var documentList: List<DocumentRevision> = database.read(0, database.documentCount, true)
            val document: DocumentRevision = documentList.elementAt(0)
            mData = UserData(document.body.asMap())
            daily = calcUserDaily(mData)

        }
        runOnUiThread(Runnable {
            if (daily == -1) {
                txtNeed.text = "Your daily need of calories can't be calculated"
                txtPerDay.text=""
                txtCal.text=""
            }
            else {
                txtCal.text = daily.toString()
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.settingsOpen){
            if (mData == null) {
                var database = mUserStore!!.database();
                var documentList: List<DocumentRevision> = database.read(0, database.documentCount, true)
                for (document in documentList) {
                    mData = UserData(document.body.asMap())
                }
            }
            val dataMap: HashMap<String, Any> = UserData.CreateMap(mData.getSex(), mData.getHeight(), mData.getWeight(), mData.getAge())
            val i = Intent(this, SettingsActivity::class.java)
            i.putExtra(SettingsActivity.EXTRA_DATA, dataMap)
            startActivity(i)
    }
    return super.onOptionsItemSelected(item)
    }
    private fun calcUserDaily(mUser: UserData): Int {
        if (mData.getSex() == "Male")
            return (9.99*mData.getWeight()+6.25*mData.getHeight()-4.92*mData.getAge()+5).roundToInt()
        else if (mData.getSex() == "Female")
            return (9.99*mData.getWeight()+6.25*mData.getHeight()-4.92*mData.getAge()-161).roundToInt()
        else return -1
    }
}
