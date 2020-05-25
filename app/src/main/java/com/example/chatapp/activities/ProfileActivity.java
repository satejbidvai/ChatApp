package com.example.chatapp.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.CustomDialog;
import com.example.chatapp.databinding.ActivityProfileBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private static final int NO_REQUESTS = 0;
    private static final int REQUEST_SENT = 1;
    private static final int REQUEST_RECEIVED = 2;
    private static final int REQUEST_ACCEPTED = 3;
    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase, mDatabaseFriendRequests, mDatabaseFriends;
    private CustomDialog dialog;
    private int friendship;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final String showUserID = getIntent().getStringExtra("userID");

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(showUserID);
        mDatabaseFriendRequests = FirebaseDatabase.getInstance().getReference().child("Friends Requests");
        mDatabaseFriends = FirebaseDatabase.getInstance().getReference().child("Friends");


        dialog = new CustomDialog(ProfileActivity.this);
        dialog.startDialog();

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue(String.class);
                String status = dataSnapshot.child("status").getValue(String.class);
                String image = dataSnapshot.child("image").getValue(String.class);

                binding.userName.setText(name);
                binding.userStatus.setText(status);

                Picasso.get().load(image).into(binding.userImage);

                mDatabaseFriendRequests.child(mAuth.getCurrentUser().getUid()).child(showUserID).child("request")
                        .addValueEventListener(new ValueEventListener() {

                            @SuppressLint("SetTextI18n")
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                if (dataSnapshot.getValue() == null) {
                                    friendship = NO_REQUESTS;
                                } else {
                                    switch (Objects.requireNonNull(dataSnapshot.getValue(String.class))) {
                                        case "sent":
                                            binding.sendFriendRequest.setText("Cancel Friend Request");
                                            friendship = REQUEST_SENT;
                                            break;

                                        case "received":
                                            binding.sendFriendRequest.setText("Accept Friend Request");
                                            binding.declineFriendRequest.setVisibility(View.VISIBLE);
                                            friendship = REQUEST_RECEIVED;
                                            break;

                                        case "accepted":
                                            binding.sendFriendRequest.setText("Remove Friend");
                                            binding.declineFriendRequest.setVisibility(View.GONE);
                                            friendship = REQUEST_ACCEPTED;
                                            break;

                                        default:
                                            break;
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                dialog.dismissDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        binding.sendFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                binding.sendFriendRequest.setEnabled(false);

                if (friendship == NO_REQUESTS) {
                    mDatabaseFriendRequests.child(mAuth.getCurrentUser().getUid()).child(showUserID).child("request").setValue("sent")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()) {
                                        mDatabaseFriendRequests.child(showUserID).child(mAuth.getCurrentUser().getUid()).child("request").setValue("received")
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {

                                                    @SuppressLint("SetTextI18n")
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        friendship = REQUEST_SENT;
                                                        binding.sendFriendRequest.setEnabled(true);
                                                        binding.sendFriendRequest.setText("Cancel Friend Request");
                                                        Snackbar.make(binding.layout, "Friend Request Sent", Snackbar.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                }

                if (friendship == REQUEST_SENT) {
                    mDatabaseFriendRequests.child(mAuth.getCurrentUser().getUid()).child(showUserID).removeValue()
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()) {
                                        mDatabaseFriendRequests.child(showUserID).child(mAuth.getCurrentUser().getUid()).removeValue()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {

                                                    @SuppressLint("SetTextI18n")
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        friendship = NO_REQUESTS;
                                                        binding.sendFriendRequest.setEnabled(true);
                                                        binding.sendFriendRequest.setText("Send Friend Request");
                                                        Snackbar.make(binding.layout, "Friend Request Cancelled", Snackbar.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            });
                }

                if (friendship == REQUEST_RECEIVED) {
                    final String currDate = DateFormat.getDateTimeInstance().format(new Date());

                    mDatabaseFriends.child(mAuth.getCurrentUser().getUid()).child(showUserID).setValue(currDate)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mDatabaseFriends.child(showUserID).child(mAuth.getCurrentUser().getUid()).setValue(currDate)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    mDatabaseFriendRequests.child(mAuth.getCurrentUser().getUid()).child(showUserID).child("request").setValue("accepted")
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    if (task.isSuccessful()) {
                                                                        mDatabaseFriendRequests.child(showUserID).child(mAuth.getCurrentUser().getUid()).child("request").setValue("accepted")
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {

                                                                                    @SuppressLint("SetTextI18n")
                                                                                    @Override
                                                                                    public void onSuccess(Void aVoid) {
                                                                                        friendship = REQUEST_ACCEPTED;
                                                                                        binding.declineFriendRequest.setVisibility(View.GONE);
                                                                                        binding.sendFriendRequest.setEnabled(true);
                                                                                        binding.sendFriendRequest.setText("Remove Friend");
                                                                                    }
                                                                                });
                                                                    }
                                                                }
                                                            });
                                                    Snackbar.make(binding.layout, "You are now Friends!!", Snackbar.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });
                }

                if (friendship == REQUEST_ACCEPTED) {
                    mDatabaseFriends.child(mAuth.getCurrentUser().getUid()).child(showUserID).removeValue()
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    mDatabaseFriends.child(showUserID).child(mAuth.getCurrentUser().getUid()).removeValue()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    mDatabaseFriendRequests.child(mAuth.getCurrentUser().getUid()).child(showUserID).removeValue()
                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                    if (task.isSuccessful()) {
                                                                        mDatabaseFriendRequests.child(showUserID).child(mAuth.getCurrentUser().getUid()).removeValue()
                                                                                .addOnSuccessListener(new OnSuccessListener<Void>() {

                                                                                    @SuppressLint("SetTextI18n")
                                                                                    @Override
                                                                                    public void onSuccess(Void aVoid) {
                                                                                        friendship = NO_REQUESTS;
                                                                                        binding.declineFriendRequest.setVisibility(View.GONE);
                                                                                        binding.sendFriendRequest.setEnabled(true);
                                                                                        binding.sendFriendRequest.setText("Send Friend Request");
                                                                                    }
                                                                                });
                                                                    }
                                                                }
                                                            });
                                                    Snackbar.make(binding.layout, "You are no longer Friends", Snackbar.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });
                }
            }
        });
    }
}
