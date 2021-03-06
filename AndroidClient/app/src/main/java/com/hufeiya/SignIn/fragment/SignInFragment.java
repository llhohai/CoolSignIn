/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hufeiya.SignIn.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hufeiya.SignIn.R;
import com.hufeiya.SignIn.activity.CategorySelectionActivity;
import com.hufeiya.SignIn.adapter.AvatarAdapter;
import com.hufeiya.SignIn.helper.PreferencesHelper;
import com.hufeiya.SignIn.helper.TransitionHelper;
import com.hufeiya.SignIn.model.Avatar;
import com.hufeiya.SignIn.model.User;
import com.hufeiya.SignIn.net.AsyncHttpHelper;

/**
 * Enable selection of an {@link Avatar} and user name.
 */
public class SignInFragment extends Fragment {

    private static final String ARG_EDIT = "EDIT";
    private static final String KEY_SELECTED_AVATAR_INDEX = "selectedAvatarIndex";
    private User mUser;
    private EditText phone;
    private EditText pass;
    private Avatar mSelectedAvatar = Avatar.ONE;
    private View mSelectedAvatarView;
    private GridView mAvatarGrid;
    private FloatingActionButton mDoneFab;
    private boolean edit;
    public ProgressBar progressBar;

    public static SignInFragment newInstance(boolean edit) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_EDIT, edit);
        SignInFragment fragment = new SignInFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            int savedAvatarIndex = savedInstanceState.getInt(KEY_SELECTED_AVATAR_INDEX);
            mSelectedAvatar = Avatar.values()[savedAvatarIndex];
        }
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View contentView = inflater.inflate(R.layout.fragment_sign_in, container, false);
        contentView.addOnLayoutChangeListener(new View.
                OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                setUpGridView(getView());
            }
        });
        return contentView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_AVATAR_INDEX, mSelectedAvatar.ordinal());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        assurePlayerInit();
        checkIsInEditMode();

        if (null == mUser || edit) {
            view.findViewById(R.id.empty).setVisibility(View.GONE);
            view.findViewById(R.id.content).setVisibility(View.VISIBLE);
            initContentViews(view);
            initContents();
        } else {
            final Activity activity = getActivity();
            CategorySelectionActivity.start(activity, mUser);
            activity.finish();
        }
        super.onViewCreated(view, savedInstanceState);
    }

    private void checkIsInEditMode() {
        final Bundle arguments = getArguments();
        //noinspection SimplifiableIfStatement
        if (null == arguments) {
            edit = false;
        } else {
            edit = arguments.getBoolean(ARG_EDIT, false);
        }
    }

    private void initContentViews(View view) {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                /* no-op */
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // showing the floating action button if text is entered
                if (s.length() == 0) {
                    mDoneFab.hide();
                } else {
                    mDoneFab.show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                /* no-op */
            }
        };
        progressBar = (ProgressBar)view.findViewById(R.id.empty);
        phone = (EditText) view.findViewById(R.id.phone);
        phone.addTextChangedListener(textWatcher);
        pass = (EditText) view.findViewById(R.id.pass);
        pass.addTextChangedListener(textWatcher);
        mDoneFab = (FloatingActionButton) view.findViewById(R.id.done);
        mDoneFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.done:


                        progressBar.setVisibility(View.VISIBLE);

                        AsyncHttpHelper.login(phone.getText().toString(),pass.getText().toString(),SignInFragment.this);
                        break;
                    default:
                        throw new UnsupportedOperationException(
                                "The onClick method has not been implemented for " + getResources()
                                        .getResourceEntryName(v.getId()));
                }
            }
        });

    }

    private void removeDoneFab(@Nullable Runnable endAction) {
        ViewCompat.animate(mDoneFab)
                .scaleX(0)
                .scaleY(0)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(endAction)
                .start();
    }

    private void setUpGridView(View container) {
        mAvatarGrid = (GridView) container.findViewById(R.id.avatars);
        mAvatarGrid.setAdapter(new AvatarAdapter(getActivity()));
        mAvatarGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedAvatarView = view;
                mSelectedAvatar = Avatar.values()[position];
            }
        });
        mAvatarGrid.setNumColumns(calculateSpanCount());
        mAvatarGrid.setItemChecked(mSelectedAvatar.ordinal(), true);
    }


    private void performSignInWithTransition(View v) {
        final Activity activity = getActivity();

        final Pair[] pairs = TransitionHelper.createSafeTransitionParticipants(activity, true,
                new Pair<>(v, activity.getString(R.string.transition_avatar)));
        @SuppressWarnings("unchecked")
        ActivityOptionsCompat activityOptions = ActivityOptionsCompat
                .makeSceneTransitionAnimation(activity, pairs);
        CategorySelectionActivity.start(activity, mUser, activityOptions);
    }

    private void initContents() {
        assurePlayerInit();
        if (null != mUser) {
            phone.setText(mUser.getPhone());
            pass.setText(mUser.getPass());
            mSelectedAvatar = mUser.getAvatar();
        }
    }

    private void assurePlayerInit() {
        if (null == mUser) {
            mUser = PreferencesHelper.getPlayer(getActivity());
        }
    }

    public void savePlayer(String md5pass) {
        mUser = new User(phone.getText().toString(), md5pass,
                mSelectedAvatar);
        PreferencesHelper.writeToPreferences(getActivity(), mUser);
    }

    /**
     * Calculates spans for avatars dynamically.
     *
     * @return The recommended amount of columns.
     */
    private int calculateSpanCount() {
        int avatarSize = getResources().getDimensionPixelSize(R.dimen.size_fab);
        int avatarPadding = getResources().getDimensionPixelSize(R.dimen.spacing_double);
        return mAvatarGrid.getWidth() / (avatarSize + avatarPadding);
    }

    public void toastLoginFail(String failType){
        if (failType.equals("account")){
            Toast.makeText(getActivity(),"蛤 (@[]@!!),账号或密码错误",Toast.LENGTH_SHORT).show();
        }else if(failType.equals("unknown")){
            Toast.makeText(getActivity(),"登录失败, ( ° △ °|||)︴ ,主人请检查下网络",Toast.LENGTH_SHORT).show();
        }else if (failType.equals("json")){
            Toast.makeText(getActivity(),"json解析错误,这是个bug额(⊙o⊙)…",Toast.LENGTH_SHORT).show();
        }

    }
    public void enterTheCategorySelectionActivity(){
        removeDoneFab(new Runnable() {
            @Override
            public void run() {
                if (null == mSelectedAvatarView) {
                    performSignInWithTransition(mAvatarGrid.getChildAt(
                            mSelectedAvatar.ordinal()));
                } else {
                    performSignInWithTransition(mSelectedAvatarView);
                }
            }
        });
    }

}
