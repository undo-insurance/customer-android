package com.kustomer.kustomersdk.Views;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.bxlargeimageviewer.BxImageViewer;
import com.kustomer.kustomersdk.Helpers.KUSLog;
import com.kustomer.kustomersdk.Helpers.KUSPermission;
import com.kustomer.kustomersdk.Kustomer;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.Utils.KUSConstants;
import com.kustomer.kustomersdk.Utils.KUSUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Junaid on 3/13/2018.
 */

public class KUSLargeImageViewer implements View.OnClickListener {

    //region Properties
    private View header;
    private ImageView ivClose;
    private ImageView ivShare;
    private TextView tvheader;
    private BxImageViewer imageViewer;

    private Context mContext;
    private String currentImageLink;
    //endregion

    //region LifeCycle
    public KUSLargeImageViewer(Context context) {
        mContext = context;

        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (layoutInflater == null)
            return;

        header = layoutInflater.inflate(R.layout.kus_large_image_viewer_header, null);

        ivClose = header.findViewById(R.id.ivClose);
        ivShare = header.findViewById(R.id.ivShare);
        tvheader = header.findViewById(R.id.tvHeader);

        ivClose.setOnClickListener(this);
        ivShare.setOnClickListener(this);
    }
    //endregion

    public void showImages(final List<String> imageURIs, int startingIndex) {

        currentImageLink = imageURIs.get(startingIndex);

        imageViewer = BxImageViewer.getInstance(mContext);
        imageViewer.initialization()
                .setImageChangeListener(new BxImageViewer.OnImageChangeListener() {
                    @Override
                    public void onImageChanged(int position) {
                        tvheader.setText(String.format(Locale.getDefault(), "%d/%d", position + 1, imageURIs.size()));
                        currentImageLink = imageURIs.get(position);
                    }
                })
                .setImageMarginPx((int) KUSUtils.dipToPixels(mContext, 20.0f))
                .addDataSet(imageURIs)
                .setStartPosition(startingIndex)
                .setOverlayView(header)
                .show();

    }

    private void shareImage() {

        Matcher urlMatcher = Pattern.compile(KUSConstants.Pattern.URL_PATTERN).matcher(currentImageLink);
        if (!urlMatcher.matches()) {
            Uri bitmapUri = Uri.parse(currentImageLink);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
            mContext.startActivity(Intent.createChooser(intent,
                    mContext.getResources().getString(R.string.com_kustomer_share_via)));
        } else {
            GlideUrl glideUrl = new GlideUrl(currentImageLink, new LazyHeaders.Builder()
                    .addHeader(KUSConstants.Keys.K_KUSTOMER_TRACKING_TOKEN_HEADER_KEY, Kustomer.getSharedInstance().getUserSession().getTrackingTokenDataSource().getCurrentTrackingToken())
                    .build());

            Glide.with(mContext)
                    .asBitmap()
                    .load(glideUrl)
                    .into(new SimpleTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (!KUSPermission.isStoragePermissionAvailable(mContext)) {
                                    Toast.makeText(mContext,
                                            mContext.getResources().getString(R.string.com_kustomer_please_provide_storage_permission),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    String path = saveBitmap(resource);
                                    shareImage(path);
                                }
                            } else {
                                String path = saveBitmap(resource);
                                shareImage(path);
                            }

                        }
                    });
        }

    }

    private String saveBitmap(Bitmap resource) {
        String bitmapPath = null;
        try {
            File file = createImageFile();
            FileOutputStream out = new FileOutputStream(file);
            resource.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            bitmapPath = Uri.fromFile(file).toString();
        } catch (IOException e) {
            KUSLog.KUSLogError(e.getMessage());
        }

        return bitmapPath;
    }

    private void shareImage(String imagePath) {
        if (imagePath != null) {
            imagePath = imagePath.replaceFirst("file://", "");
            Uri bitmapUri = KUSUtils.getUriFromFile(mContext, new File(imagePath));

            if (bitmapUri != null) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(Intent.EXTRA_STREAM, bitmapUri);
                mContext.startActivity(Intent.createChooser(intent,
                        mContext.getResources().getString(R.string.com_kustomer_share_via)));
            } else {
                Toast.makeText(Kustomer.getContext(),
                        Kustomer.getContext().getString(R.string.com_kustomer_unable_to_share_file),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    @Override
    public void onClick(View v) {
        if (v == ivClose) {
            if (imageViewer != null) {
                imageViewer.onDismiss();
            }
        } else if (v == ivShare) {
            shareImage();
        }
    }
}
