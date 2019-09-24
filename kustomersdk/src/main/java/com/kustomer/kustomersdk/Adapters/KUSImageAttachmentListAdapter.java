package com.kustomer.kustomersdk.Adapters;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.kustomer.kustomersdk.Interfaces.KUSBitmapListener;
import com.kustomer.kustomersdk.Models.KUSBitmap;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.ViewHolders.KUSImageAttachmentViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Junaid on 1/19/2018.
 */

public class KUSImageAttachmentListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements KUSImageAttachmentViewHolder.ImageAttachmentListener {

    //region Properties
    private List<KUSBitmap> imageBitmaps;
    private onItemClickListener mListener;
    //endregion

    //region LifeCycle
    public KUSImageAttachmentListAdapter(onItemClickListener listener) {
        imageBitmaps = new ArrayList<>();
        mListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new KUSImageAttachmentViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kus_item_image_attachment_view_holder, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((KUSImageAttachmentViewHolder) holder).onBind(imageBitmaps.get(position), this);
    }

    @Override
    public int getItemCount() {
        return imageBitmaps.size();
    }

    public void attachImage(final KUSBitmap kusBitmap) {
        imageBitmaps.add(kusBitmap);

        kusBitmap.addListener(new KUSBitmapListener() {
            @Override
            public void onBitmapCreated() {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        notifyItemChanged(imageBitmaps.indexOf(kusBitmap));
                    }
                });
            }

            @Override
            public void onMemoryError(OutOfMemoryError memoryError) {
                for (KUSBitmap kusBitmap : imageBitmaps) {
                    if (kusBitmap.getBitmap() != null) {
                        kusBitmap.getBitmap().recycle();
                        kusBitmap.setBitmap(null);
                    }
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        removeAll();
                    }
                });
            }
        });

        notifyItemRangeInserted(imageBitmaps.indexOf(kusBitmap), 1);
    }

    public void removeAll() {
        imageBitmaps.clear();
        notifyDataSetChanged();
    }

    public List<KUSBitmap> getImageBitmaps() {
        return imageBitmaps;
    }

    private List<String> getImageUris() {
        List<String> imageUris = new ArrayList<>();
        for (KUSBitmap kusBitmap : imageBitmaps) {
            if (kusBitmap.getBitmap() != null)
                imageUris.add(kusBitmap.getUri());
        }
        return imageUris;
    }

    //endreigon

    //region Callbacks
    @Override
    public void onImageCancelClicked(KUSBitmap imageBitmap) {
        int pos = imageBitmaps.indexOf(imageBitmap);

        imageBitmaps.remove(imageBitmap);
        notifyItemRemoved(pos);
        mListener.onAttachmentImageRemoved();
    }

    @Override
    public void onImageTapped(int position) {
        mListener.onAttachmentImageClicked(position, getImageUris());
    }
    //endregion

    //region Listener
    public interface onItemClickListener {
        void onAttachmentImageClicked(int position, List<String> imageURIs);

        void onAttachmentImageRemoved();
    }
    //endregion
}
