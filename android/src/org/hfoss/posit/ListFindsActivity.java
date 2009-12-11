/*******************************************************************************
 * Copyright (c) 2009 .
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 ******************************************************************************/

package org.hfoss.posit;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import org.hfoss.posit.provider.MyDBHelper;
import org.hfoss.posit.utilities.Utils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter.ViewBinder;

public class ListFindsActivity extends ListActivity implements ViewBinder{

	private static final String TAG = "ListActivity";
	private MyDBHelper mDbHelper;
	private Cursor mCursor;  // Used for DB accesses
	
	private static final int confirm_exit=1;

	private static final int CONFIRM_DELETE_DIALOG = 0;
	public static final int FIND_FROM_LIST = 0;
	private static int PROJECT_ID;

	/** 
	 * This method is invoked when the Activity starts and
	 *  when the user navigates back to ListFindsActivity
	 *  from some other app. It creates a
	 *  DBHelper and calls fillData() to fetch data from the DB.
	 *  @param savedInstanceState contains the Activity's previously
	 *   frozen state.  In this case it is unused.
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		PROJECT_ID = sp.getInt("PROJECT_ID", 0);
		//Utils.showToast(this, "Project: " + sp.getString("PROJECT_NAME", ""));
		
		mDbHelper = new MyDBHelper(this);
		//fillData();
		//mDbHelper.close();
	}

	/** 
	 * This method is called when the activity is ready to start 
	 *  interacting with the user. It is at the top of the Activity
	 *  stack.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		PROJECT_ID = sp.getInt("PROJECT_ID", 0);
		//Utils.showToast(this, "Project: " + sp.getString("PROJECT_NAME", ""));
		
		fillData();
	}

	/**
	 * Called when the system is about to resume some other activity.
	 *  It can be used to save state, if necessary.  In this case
	 *  we close the cursor to the DB to prevent memory leaks.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onPause(){
		super.onPause();
		stopManagingCursor(mCursor);
		mDbHelper.close(); // NOTE WELL: Can't close while managing cursor
		mCursor.close();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mDbHelper.close();
		mCursor.close();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDbHelper.close();
		mCursor.close();
	}


	/**
	 * Puts the items from the DB table into the rows of the view. Note that
	 *  once you start managing a Cursor, you cannot close the DB without 
	 *  causing an error.
	 */
	private void fillData() {
		String[] columns = MyDBHelper.list_row_data;
		int [] views = MyDBHelper.list_row_views;

		//mCursor = mDbHelper.fetchAllFinds(PROJECT_ID);	
		
		Uri allFinds = Uri.parse("content://org.hfoss.provider.POSIT/finds_project/"+PROJECT_ID);
	    mCursor = managedQuery(allFinds, null, null, null, null);
	    
		if (mCursor.getCount() == 0) { // No finds
			setContentView(R.layout.list_finds);
			return;
		}
		//startManagingCursor(mCursor); // NOTE: Can't close DB while managing cursor

		// CursorAdapter binds the data in 'columns' to the views in 'views' 
		// It repeatedly calls ViewBinder.setViewValue() (see below) for each column
		SimpleCursorAdapter adapter = 
			new SimpleCursorAdapter(this, R.layout.list_row, mCursor, columns, views);
		adapter.setViewBinder(this);
		setListAdapter(adapter); 
		//stopManagingCursor(mCursor);
	}


	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * This method executes when the user clicks on one of the Finds in the
	 *   list. It starts the FindActivity in EDIT mode, which will read
	 *   the Find's data from the DB.
	 *   @param l is the ListView that was clicked on 
	 *   @param v is the View within the ListView
	 *   @param position is the View's position in the ListView
	 *   @param id is the Find's RowID
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Intent intent = new Intent(this, FindActivity.class);
		intent.setAction(Intent.ACTION_EDIT);
		intent.putExtra(MyDBHelper.COLUMN_IDENTIFIER, mDbHelper.getIdentifierFromRowId(id));
		startActivityForResult(intent, FIND_FROM_LIST);
		FindActivity.SAVE_CHECK=false;
	}

	/**
	 * This method creates the menus for this activity.
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list_finds_menu, menu);
		return true;
	}

	/** 
	 * This method is invoked when a menu item is selected. It starts
	 *   the appropriate Activity.
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {

		case R.id.sync_finds_menu_item: 
			
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
			boolean syncIsOn = sp.getBoolean("SYNC_ON_OFF", true);
			if (!syncIsOn) {
				Utils.showToast(this, "Synchronization is turned off.");
				break;
			}
			intent = new Intent(this, SyncActivity.class);
			intent.setAction(Intent.ACTION_SYNC);
			startActivity(intent);
			break;
			
		case R.id.map_finds_menu_item:
			mDbHelper.close();
			intent = new Intent(this, MapFindsActivity.class);
			startActivity(intent);
			break;
			
		case R.id.delete_finds_menu_item:
			showDialog(CONFIRM_DELETE_DIALOG);
			break;
		
		case R.id.georss:
			generateGeoRSS();
			break;
			
		}
		return true;
	}
	
	public void generateGeoRSS() {
		mCursor = mDbHelper.fetchAllFinds(PROJECT_ID);
		startManagingCursor(mCursor);

		
		try{
			FileOutputStream fout = new FileOutputStream("/data/rss/data.xml");
			PrintStream out = new PrintStream(fout);
			out.println("<feed xmlns=\"http://www.w3.org/2005/Atom\"");
			out.println("xmlns:georss=\"http://www.georss.org/georss\"");
			out.println("xmlns:gml=\"http://www.opengis.net/gml\">");
			mCursor.moveToFirst();
			while(!mCursor.isAfterLast()) {
				out.println("<entry>");
				out.println("<title>"+
					mCursor.getString(mCursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_NAME))+
					"</title>");
				out.println("<description>"+
					mCursor.getString(mCursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_DESCRIPTION))+
					"</description>");
				out.println("<georss:where>");
				out.println("<gml:Point>");
				out.println("<gml:pos>"+
					mCursor.getDouble(mCursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_LATITUDE))+" "+
					mCursor.getDouble(mCursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_LONGITUDE))+
					"</gml:pos>");
				out.println("</gml:Point>");
				out.println("</georss:where>");
				out.println("<datetime>"+
						mCursor.getString(mCursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_TIME))+
						"</datetime>");
				out.println("</entry>");
				mCursor.moveToNext();
			}
			out.println("</feed>");
			out.close();
		}
		catch(IOException e){e.printStackTrace();}
		finally{
			Utils.showToast(this, "GeoRSS created!");
		}
	}

	/**
	 * Part of ViewBinder interface. Binds the Cursor column defined 
	 * by the specified index to the specified view. When binding is handled 
	 * by this ViewBinder, this method must return true. If this method returns false, 
	 * SimpleCursorAdapter will attempt to handle the binding on its own.
	 */
	public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
		TextView tv = (TextView) view;
		long findIden = cursor.getLong(cursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_IDENTIFIER));
		switch (view.getId()) {
		/*case R.id.find_image:

			int rowId = cursor.getInt(cursor
					.getColumnIndexOrThrow(MyDBHelper.COLUMN_IDENTIFIER));
			MyDBHelper myDbHelper = new MyDBHelper(this);
			ContentValues values = myDbHelper.getImages(rowId);
			ImageView iv = (ImageView) view;
			if (values != null && values.containsKey(getString(R.string.imageUriDB))) {
				String strUri = values.getAsString(getString(R.string.imageUriDB));
				if (strUri != null) {
					Uri iUri = Uri.parse(strUri);
					//iv.setImageURI(iUri);
					iv.setScaleType(ImageView.ScaleType.FIT_XY);
				} else {
					iv.setImageResource(R.drawable.person_icon);
					iv.setScaleType(ImageView.ScaleType.FIT_XY);
				}
			} else {
				iv.setImageResource(R.drawable.person_icon);
				iv.setScaleType(ImageView.ScaleType.FIT_XY);
			}
			return true;*/
		case R.id.latitude_id:
			String lat = cursor.getString(cursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_LATITUDE));
			tv.setText("Location: "+lat);
			return true;
		case R.id.longitude_id:
			String lon = cursor.getString(cursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_LONGITUDE));
			tv.setText(", "+lon);
			return true;
		case R.id.status:
			int status = cursor.getInt(cursor.getColumnIndexOrThrow(MyDBHelper.COLUMN_SYNCED));
			tv.setText(status==1?"Synced  ":"Not synced  ");
			return true;
		case R.id.num_photos:
			Uri findPhotos = Uri.parse("content://org.hfoss.provider.POSIT/photo_findid/"+findIden);
		    int count = managedQuery(findPhotos, null, null, null, null).getCount();
			tv.setText(count+" photos  ");
			return true;
		default:
			return false;
		}
	}
	/*@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if(keyCode==KeyEvent.KEYCODE_BACK){
			showDialog(confirm_exit);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}*/
	


	/**
	 * This method is invoked by showDialog() when a dialog window is created. It displays
	 *  the appropriate dialog box, currently a dialog to confirm that the user wants to 
	 *  delete all the finds.
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		
		switch (id) {
		case CONFIRM_DELETE_DIALOG:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.alert_dialog)
			.setPositiveButton(R.string.alert_dialog_ok, 
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff 
					/*MyDBHelper mDbHelper = new MyDBHelper(ListFindsActivity.this);
					if (mDbHelper.deleteAllFinds()) {
						mDbHelper.close();
						Utils.showToast(ListFindsActivity.this, R.string.deleted_from_database);
						finish();
					} else {
						mDbHelper.close();
						Utils.showToast(ListFindsActivity.this, R.string.delete_failed);
						dialog.cancel();
					}*/
					
					int count = getContentResolver().delete(Uri.parse("content://org.hfoss.provider.POSIT/finds_project/"+PROJECT_ID), null, null);
					Log.i("PROVIDER", "deleted "+ count);
					Utils.showToast(ListFindsActivity.this, R.string.deleted_from_database);
					setContentView(R.layout.list_finds);
					
					//finish();
					//Intent intent = new Intent(ListFindsActivity.this, ListFindsActivity.class);
					//startActivity(intent);
					//TabMain.moveTab(0);
					//TabMain.moveTab(1);
				}
			}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked Cancel so do nothing */
				}
			}).create();

		} // switch
		
		switch (id) {
		case confirm_exit:
			return new AlertDialog.Builder(this)
			.setIcon(R.drawable.alert_dialog_icon)
			.setTitle(R.string.exit)
			.setPositiveButton(R.string.alert_dialog_ok, 
					new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// User clicked OK so do some stuff 
					finish();
				}
			}).setNegativeButton(R.string.alert_dialog_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					/* User clicked Cancel so do nothing */
				}
			}).create();

		default:
			return null;
		}
		}

}