/*
 *  Copyright (C) 2008-2009 Rev. Johnny Healey <rev.null@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.healeys.lexic;

import net.healeys.lexic.game.Game;
import net.healeys.lexic.view.BoardView;
import net.healeys.trie.Trie;

import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.regex.Pattern;
import java.util.Iterator;
import java.util.Set;
import java.util.LinkedHashMap;

public class OnePlayerScore extends TabActivity {

	private static final String TAG = "OnePlayerScore";

	public static final String DEFINE_URL = 
		"http://www.google.com/search?q=define%3a+";

	private Game game;
	private BoardView bv;
	private View highlighted;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
     	super.onCreate(savedInstanceState);

		if(savedInstanceState != null) {
			game = new Game(this, savedInstanceState);

		} else {
			Intent intent = getIntent();
			Bundle bun = intent.getExtras();
			game = new Game(this,bun);
		}
		game.initializeDictionary();

		// Set up the tabs
		TabHost host = getTabHost();
		LayoutInflater.from(this).inflate(R.layout.score_view,
			host.getTabContentView(), true);
		host.addTab(host.newTabSpec("found").setIndicator("Found Words").
			setContent(R.id.found_words));
		host.addTab(host.newTabSpec("missed").setIndicator("Missed Words").
			setContent(R.id.missed_words));

		bv = (BoardView) findViewById(R.id.missed_board);
		bv.setBoard(game.getBoard());

		Set<String> possible = game.getSolutions().keySet();

		ViewGroup foundVG = initializeScrollView(R.id.found_scroll);
		ViewGroup missedVG = initializeScrollView(R.id.missed_scroll);

		int score = 0;
		int max_score = 0;
		int words = 0;
		int max_words = possible.size();

		Iterator<String> li = game.uniqueListIterator();
		while(li.hasNext()) {
			String w = li.next();

			if(game.isWord(w) && game.WORD_POINTS[w.length()] > 0) {
				int points = game.WORD_POINTS[w.length()];
				addWord(foundVG,w,points,0xff000000,true);
				score += game.WORD_POINTS[w.length()];
				words++;
			} else {
				addWord(foundVG,w,0,0xffff0000,false);
			}

			possible.remove(w);
		}
	
		max_score = score;
		li = possible.iterator();

		while(li.hasNext()) {
			String w = li.next();
			max_score += game.WORD_POINTS[w.length()];
			addMissedWord(missedVG,game.getSolutions().get(w));
		}

		TextView t = (TextView) findViewById(R.id.score_points);
		t.setText(""+score+"/"+max_score);

		t = (TextView) findViewById(R.id.score_words);
		t.setText(""+words+"/"+max_words);

		Button b = (Button) findViewById(R.id.close_score);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

		b = (Button) findViewById(R.id.missed_close_score);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		game.save(outState);
	}

	private ViewGroup initializeScrollView(int resId) {
		ScrollView sv = (ScrollView) findViewById(resId);
		sv.setScrollBarStyle(sv.SCROLLBARS_OUTSIDE_INSET);

		ViewGroup.LayoutParams llLp = new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT);

		LinearLayout ll = new LinearLayout(this);
		ll.setLayoutParams(llLp);
		ll.setOrientation(LinearLayout.VERTICAL);
		sv.addView(ll);

		return ll;
	}

	private void addWord(ViewGroup vg, String w, int points, int color,
		boolean link) {
		LinearLayout.LayoutParams text1Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			(float) 1.0);
		TextView tv1 = new TextView(this);
		tv1.setGravity(Gravity.LEFT);
		tv1.setLayoutParams(text1Lp);
		tv1.setTextSize(16);
		tv1.setTextColor(color);
		tv1.setText(w);

		LinearLayout.LayoutParams text2Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT);
		TextView tv2 = new TextView(this);
		tv2.setGravity(Gravity.RIGHT);
		tv2.setLayoutParams(text2Lp);
		tv2.setTextSize(16);
		tv2.setTextColor(color);
		tv2.setText(""+points+" ");

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);

		ll.addView(tv1);

		if(link) {
			TextView tv3 = new TextView(this);
			tv3.setGravity(Gravity.RIGHT);
			tv3.setLayoutParams(text2Lp);
			tv3.setTextSize(12);
			tv3.setTextColor(0xff000000);
			tv3.setText(R.string.define_word);

			tv3.setOnClickListener(new DefinerListener(w));
			tv3.setFocusable(true);

			ll.addView(tv3);
		}

		ll.addView(tv2);

		vg.addView(ll, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT));

	}

	private void addMissedWord(ViewGroup vg, Trie.Solution solution) {
		String w = solution.getWord();

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.HORIZONTAL);

		ViewGroup.LayoutParams text1Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT,
			(float) 1.0);
		TextView tv1 = new TextView(this);
		tv1.setGravity(Gravity.LEFT);
		tv1.setLayoutParams(text1Lp);
		tv1.setTextSize(16);
		tv1.setTextColor(0xff000000);
		tv1.setText(w);
		
		ViewGroup.LayoutParams text2Lp = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT,
			ViewGroup.LayoutParams.WRAP_CONTENT);

		// The Word Highlighting Link
		TextView tv2 = new TextView(this);
		tv2.setGravity(Gravity.RIGHT);
		tv2.setLayoutParams(text2Lp);
		tv2.setTextSize(12);
		tv2.setTextColor(0xff000000);
		tv2.setText(R.string.view_word);

		tv2.setOnClickListener(new HighlighterListener(solution.getMask(),
			ll));
		tv2.setFocusable(true);

		// The Definition Link
		TextView tv3 = new TextView(this);
		tv3.setGravity(Gravity.RIGHT);
		tv3.setLayoutParams(text2Lp);
		tv3.setTextSize(12);
		tv3.setTextColor(0xff000000);
		tv3.setText(R.string.define_word);

		tv3.setOnClickListener(new DefinerListener(w));
		tv3.setFocusable(true);

		// Padding between the links
		TextView padding = new TextView(this);
		padding.setGravity(Gravity.RIGHT);
		padding.setLayoutParams(text2Lp);
		padding.setTextSize(12);
		padding.setText("        ");

		ll.addView(tv1);
		ll.addView(tv2);
		ll.addView(padding);
		ll.addView(tv3);
			
		vg.addView(ll, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.FILL_PARENT,
			ViewGroup.LayoutParams.WRAP_CONTENT));
	}

	private class HighlighterListener implements View.OnClickListener {
		private int mask;
		private View parentView;

		private HighlighterListener(int mask, View parentView) {
			this.mask = mask;
			this.parentView = parentView;
		}

		public void onClick(View v) {
			// Log.d(TAG,"highlighter listener:"+mask);
			bv.highlight(mask);
			bv.invalidate();

			if(highlighted != null) {
				highlighted.setBackgroundColor(0x00000000);
			}
			highlighted = parentView;
			highlighted.setBackgroundColor(0xffffff00);
		}
	}

	private class DefinerListener implements View.OnClickListener {
		String word;

		private DefinerListener(String word) {
			this.word = word;
		}

		public void onClick(View v) {
			Intent i = new Intent(Intent.ACTION_VIEW);
			Uri u = Uri.parse(DEFINE_URL+word);
			i.setData(u);
			startActivity(i);
		}
	}

}

