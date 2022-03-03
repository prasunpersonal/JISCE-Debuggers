package com.prasunpersonal.jiscedebuggers.Activities;

import static com.prasunpersonal.jiscedebuggers.App.ME;
import static com.prasunpersonal.jiscedebuggers.App.NOTIFICATION_CHANNEL_ID;
import static com.prasunpersonal.jiscedebuggers.App.getFileSize;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import com.prasunpersonal.jiscedebuggers.Adapters.ImgAdapter;
import com.prasunpersonal.jiscedebuggers.Adapters.ReportAdapter;
import com.prasunpersonal.jiscedebuggers.Models.Report;
import com.prasunpersonal.jiscedebuggers.R;
import com.prasunpersonal.jiscedebuggers.databinding.ActivityReportBinding;

public class ReportActivity extends AppCompatActivity {
    private static final String TAG = ReportActivity.class.getSimpleName();
    private final StorageReference STORAGE_REFERENCE = FirebaseStorage.getInstance().getReference("JISCE_Debuggers").child("Reports");
    ActivityReportBinding binding;
    FirebaseFirestore db;
    ArrayList<String> uris;
    ArrayList<Report> reports;

    ActivityResultLauncher<Intent> uploadImgResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            if (result.getData().getClipData() != null) {
                for (int i = 0; i < result.getData().getClipData().getItemCount(); i++) {
                    if (!uris.contains(result.getData().getClipData().getItemAt(i).getUri().toString())) {
                        uris.add(result.getData().getClipData().getItemAt(i).getUri().toString());
                        if (binding.reportSS.getAdapter() != null) {
                            binding.reportSS.getAdapter().notifyItemInserted(uris.size() - 1);
                        }
                    }
                }
            } else {
                if (!uris.contains(result.getData().getData().toString())) {
                    uris.add(result.getData().getData().toString());
                    if (binding.reportSS.getAdapter() != null) {
                        binding.reportSS.getAdapter().notifyItemInserted(uris.size() - 1);
                    }
                }
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        db = FirebaseFirestore.getInstance();
        uris = new ArrayList<>();
        reports = new ArrayList<>();

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.reportSS.setLayoutManager(new GridLayoutManager(this, 4));
        binding.reportSS.setAdapter(new ImgAdapter(uris, true));

        binding.prevReports.setLayoutManager(new LinearLayoutManager(this));
        binding.prevReports.setAdapter(new ReportAdapter(reports));

        binding.upload.setOnClickListener(v1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            uploadImgResult.launch(intent);
        });

        FirebaseFirestore.getInstance().collection("Reports").whereEqualTo("userId", ME.getUserID()).orderBy("time").addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.d(TAG, "onViewCreated: ", error);
                return;
            }

            if (value != null) {
                for (DocumentChange dc : value.getDocumentChanges()) {
                    DocumentChange.Type type = dc.getType();
                    if (type == DocumentChange.Type.ADDED) {
                        reports.add(0, dc.getDocument().toObject(Report.class));
                        if (binding.prevReports.getAdapter() != null) {
                            binding.prevReports.getAdapter().notifyItemInserted(0);
                        }
                    } else if (type == DocumentChange.Type.REMOVED) {
                        int index = reports.indexOf(dc.getDocument().toObject(Report.class));
                        reports.remove(index);
                        if (binding.prevReports.getAdapter() != null) {
                            binding.prevReports.getAdapter().notifyItemRemoved(index);
                        }
                    } else if (type == DocumentChange.Type.MODIFIED) {
                        int index = reports.indexOf(dc.getDocument().toObject(Report.class));
                        reports.set(index, dc.getDocument().toObject(Report.class));
                        if (binding.prevReports.getAdapter() != null) {
                            binding.prevReports.getAdapter().notifyItemChanged(index);
                        }
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_report, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.done) {
            if (binding.repoertTitle.getText().toString().trim().isEmpty()) {
                binding.repoertTitle.setError("Name the error page!");
            } else if (binding.reportDescription.getText().toString().trim().isEmpty()) {
                binding.reportDescription.setError("Describe the error!");
            } else {
                uploadPost(new ArrayList<>(uris), binding.repoertTitle.getText().toString().trim(), binding.reportDescription.getText().toString().trim(), new ArrayList<>(), 0);uris.clear();
                binding.repoertTitle.setText(null);
                binding.reportDescription.setText(null);
                if (binding.reportSS.getAdapter() != null) binding.reportSS.getAdapter().notifyDataSetChanged();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void uploadPost(ArrayList<String> uris, String title, String description, ArrayList<String> images, final int i) {
        if (uris.isEmpty()) {
            Report report = new Report(ME.getUserID(), title, description, images);
            db.collection("Reports").document(report.getReportId()).set(report);
        } else {
            if (i == uris.size()) {
                Report report = new Report(ME.getUserID(), title, description, images);
                db.collection("Reports").document(report.getReportId()).set(report);
            } else {
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Uploading Images")
                        .setOngoing(true)
                        .setOnlyAlertOnce(true);
                notificationManager.notify(1, builder.build());

                STORAGE_REFERENCE.child(ME.getUserID()).child(String.valueOf(i)).putFile(Uri.parse(uris.get(i))).addOnSuccessListener(taskSnapshot -> STORAGE_REFERENCE.child(ME.getUserID()).child(String.valueOf(i)).getDownloadUrl().addOnSuccessListener(uri -> {
                    images.add(uri.toString());
                    uploadPost(uris, title, description, images, i + 1);
                })).addOnProgressListener(snapshot -> {
                    builder.setContentText(getFileSize(snapshot.getBytesTransferred()) + "/" + getFileSize(snapshot.getTotalByteCount())).setProgress((int) snapshot.getTotalByteCount(), (int) snapshot.getBytesTransferred(), false);
                    notificationManager.notify(1, builder.build());
                    if (snapshot.getBytesTransferred() == snapshot.getTotalByteCount()) {
                        notificationManager.cancel(1);
                    }
                });
            }
        }
    }
}