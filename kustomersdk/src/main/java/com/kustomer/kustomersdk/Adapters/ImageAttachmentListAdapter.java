package com.kustomer.kustomersdk.Adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.kustomer.kustomersdk.Interfaces.BitmapListener;
import com.kustomer.kustomersdk.Models.KUSBitmap;
import com.kustomer.kustomersdk.R;
import com.kustomer.kustomersdk.ViewHolders.ImageAttachmentViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Junaid on 1/19/2018.
 */

public class ImageAttachmentListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements ImageAttachmentViewHolder.ImageAttachmentListener {

    //region Properties
    private List<String> imageURIs;
    private List<KUSBitmap> imageBitmaps;
    private onItemClickListener mListener;
    //endregion

    //region LifeCycle
    public ImageAttachmentListAdapter(onItemClickListener listener) {
        imageURIs = new ArrayList<>();
        imageBitmaps = new ArrayList<>();
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ImageAttachmentViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.kus_item_image_attachment_view_holder, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((ImageAttachmentViewHolder) holder).onBind(imageBitmaps.get(position), this);
    }

    @Override
    public int getItemCount() {
        return imageBitmaps.size();
    }

    public void attachImage(final KUSBitmap kusBitmap) {
        imageURIs.add(kusBitmap.getUri());
        imageBitmaps.add(kusBitmap);

        kusBitmap.addBitmapListeners(new BitmapListener() {
            @Override
            public void onBitmapCreated() {
                notifyItemChanged(imageBitmaps.indexOf(kusBitmap));
            }
        });

        notifyItemRangeInserted(imageBitmaps.indexOf(kusBitmap), 1);
    }

    public void removeAll() {
        imageURIs.clear();
        imageBitmaps.clear();
        notifyDataSetChanged();
    }

    public List<String> getImageURIs() {
        return imageURIs;
    }

    public List<KUSBitmap> getImageBitmaps() {
        return imageBitmaps;
    }

    //endreigon

    //region Callbacks
    @Override
    public void onImageCancelClicked(KUSBitmap imageBitmap) {
        int pos = imageBitmaps.indexOf(imageBitmap);

        imageURIs.remove(imageBitmap.getUri());
        imageBitmaps.remove(imageBitmap);
        notifyItemRemoved(pos);
        mListener.onAttachmentImageRemoved();
    }

    @Override
    public void onImageTapped(int position) {
        mListener.onAttachmentImageClicked(position, imageURIs);
    }
    //endregion

    //region Listener
    public interface onItemClickListener {
        void onAttachmentImageClicked(int position, List<String> imageURIs);

        void onAttachmentImageRemoved();
    }
    //endregion
}
