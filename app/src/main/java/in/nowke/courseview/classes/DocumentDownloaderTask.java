package in.nowke.courseview.classes;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import in.nowke.courseview.AddDocumentActivity;
import in.nowke.courseview.adapters.CourseviewDBAdapter;
import in.nowke.courseview.model.Document;
import in.nowke.courseview.model.Subject;

/**
 * Created by nav on 23/12/15.
 */
public class DocumentDownloaderTask extends AsyncTask<Integer, String, String> {

    private ProgressDialog progressDialog;
    private Context context;

    public DocumentDownloaderTask(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Download");
        progressDialog.setMessage("Fetching document...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    protected String doInBackground(Integer... documentId) {
        String downloadURL = Constants.getSubjectsForDocument(documentId[0]);
        try {
            URL documentURL = new URL(downloadURL);
            InputStream input = new BufferedInputStream(documentURL.openStream(), 8192);
            String fileContent = Helpers.convertStreamToString(input);

           return fileContent;

        } catch (IOException  e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String fileContent) {

        if (progressDialog != null ) {
            progressDialog.setMessage("Writing contents...");
        }
        try {
            JSONArray documentWrapper = new JSONArray(fileContent);
            JSONObject documentListWrapper = documentWrapper.getJSONObject(0);
            JSONArray subjectList = documentListWrapper.getJSONArray("subjects");

            List<Subject> subjects = new ArrayList<>();

            for (int i=0; i < subjectList.length(); i++) {
                Subject subject = new Subject();
                JSONObject subjectObj = subjectList.getJSONObject(i);

                subject.code = subjectObj.getString("code");
                subject.credits = subjectObj.getDouble("credits");
                subject.title = subjectObj.getString("title");
                subject.content = subjectObj.getString("content");

                subjects.add(subject);
            }

            // Database
            CourseviewDBAdapter helper = new CourseviewDBAdapter(context);

            // Create document
            Document document = new Document();
            document.title = documentListWrapper.getString("title");
            document.owner = documentListWrapper.getString("owner");
            long documentId = helper.addDocument(document);

            // Fill subjects
            long curSubId = helper.addSubjects(subjects, documentId);
            helper.updateCurrentSubjectToDocument(documentId, curSubId);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
