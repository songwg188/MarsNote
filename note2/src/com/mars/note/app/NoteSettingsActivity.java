package com.mars.note.app;

import com.mars.note.R;
import com.mars.note.api.BaseActivity;
import com.mars.note.api.FragmentFactory;
import com.mars.note.fragment.NoteSettingsFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;

public class NoteSettingsActivity extends BaseActivity {
	private ListView mListView;
	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;
	private NoteSettingsFragment mNoteSettingsMenu;
	public static final int BACKUP_RESTORE = 1;
	private BackUpActivity mBackUpAndRestore;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_note_settings);
		mFragmentManager = this.getFragmentManager();
		mNoteSettingsMenu = FragmentFactory.newSettingsFragment();
		mBackUpAndRestore = new BackUpActivity();
		mFragmentTransaction = mFragmentManager.beginTransaction();
		mFragmentTransaction.add(R.id.root, mNoteSettingsMenu,
				"mNoteSettingsMenu").commit();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			this.onBackPressed();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
		overridePendingTransition(android.R.anim.fade_in,
				android.R.anim.fade_out);
	}
}
