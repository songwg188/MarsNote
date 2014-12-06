package com.mars.note;

import java.io.FileNotFoundException;
import java.io.IOException;
import com.mars.note.fragment.NoteSettingsMenu;
import com.mars.note.utils.PictureHelper;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;

public class NoteSettings extends Activity {
	ActionBar mActionBar;
	ListView mListView;
	FragmentManager mFragmentManager;
	FragmentTransaction mFragmentTransaction;
	NoteSettingsMenu mNoteSettingsMenu;
	public static final int BACKUP_RESTORE = 1;
	BackUpAndRestore mBackUpAndRestore;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY); //Ðü¸¡Actionbar 20141202
		
		mActionBar = getActionBar();
		mActionBar.setDisplayShowHomeEnabled(false);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		this.setContentView(R.layout.activity_note_settings);
		mFragmentManager = this.getFragmentManager();
		mNoteSettingsMenu = new NoteSettingsMenu();
		mBackUpAndRestore = new BackUpAndRestore();
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
