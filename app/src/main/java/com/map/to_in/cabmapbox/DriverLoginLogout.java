package com.map.to_in.cabmapbox;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginLogout extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private static FirebaseAuth firebaseAuth;
    private static FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_logout);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

        firebaseAuth = FirebaseAuth.getInstance();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    Intent intent = new Intent(DriverLoginLogout.this, DriverMapActivity.class);
                    startActivity(intent);
                    return;
                }
            }
        };
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_driver_login_logout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_driver_login_logout, container, false);

            ImageView imageView;
            EditText email, password;
            View rootView = null;
            TextView cons = null;
            switch (getArguments().getInt(ARG_SECTION_NUMBER)){
                case 1:
                    rootView = inflater.inflate(R.layout.fragment_login_, container, false);

                    imageView = rootView.findViewById(R.id.imageView2);
                    imageView.setImageResource(R.drawable.school_schoolbus);

                    cons = rootView.findViewById(R.id.constant);
                    cons.setText(getString(R.string.driver));

                    email = rootView.findViewById(R.id.email);
                    password = rootView.findViewById(R.id.password);
                    Button login = rootView.findViewById(R.id.login);

                    login.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String emailID = email.getText().toString();
                            final String pass = password.getText().toString();
                            firebaseAuth.signInWithEmailAndPassword(emailID, pass)
                                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if(!task.isSuccessful()) {
                                            Toast.makeText(getActivity(), "Sign In Error", Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(getActivity(), "Logging In", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                        }
                    });
                    break;
                case 2:
                    rootView = inflater.inflate(R.layout.fragment_registration_, container, false);

                    imageView = rootView.findViewById(R.id.imageView2);
                    imageView.setImageResource(R.drawable.school_schoolbus);

                    cons = rootView.findViewById(R.id.constant);
                    cons.setText(getString(R.string.driver));

                    email = rootView.findViewById(R.id.email);
                    password = rootView.findViewById(R.id.password);
                    Button registration = rootView.findViewById(R.id.registration);

                    registration.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String emailID = email.getText().toString();
                            final String pass = password.getText().toString();
                            firebaseAuth.createUserWithEmailAndPassword(emailID, pass)
                                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        if(!task.isSuccessful()) {
                                            Toast.makeText(getActivity(), "Sign Up Error", Toast.LENGTH_SHORT).show();
                                        }else{
                                            String user_id = firebaseAuth.getCurrentUser().getUid();
                                            DatabaseReference current_user_db = FirebaseDatabase.getInstance()
                                                    .getReference().child("Users").child("Customer").child(user_id);
                                            current_user_db.setValue(true);
                                            Toast.makeText(getActivity(), "User Id Created.", Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                });
                        }
                    });

                    break;
            }

            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 1:
                    return "login";
                case 2:
                    return "logout";
            }
            return null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
