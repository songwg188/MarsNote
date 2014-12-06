package com.mars.note.provider;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class MyRemoteViewsService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		// TODO Auto-generated method stub
		return new MyRemoteViewsFactory(this.getApplicationContext(), intent);
	}

}
