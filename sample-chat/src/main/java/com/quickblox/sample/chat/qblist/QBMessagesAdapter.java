package com.quickblox.sample.chat.qblist;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBAttachment;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.sample.chat.R;
import com.quickblox.users.model.QBUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QBMessagesAdapter extends RecyclerView.Adapter<QBMessagesAdapter.ViewHolder> implements QBBaseAdapter<QBChatMessage> {
    private static final String TAG = QBMessagesAdapter.class.getSimpleName();
    private List<Integer> customView;


    protected enum ViewTypes {TYPE_OWN_MESSAGE, TYPE_OPPONENT_MESSAGE, TYPE_ATTACHMENT_MESSAGE_OWN, TYPE_ATTACHMENT_MESSAGE_OPPONENT, TYPE_ATTACHMENT_CUSTOM}

    private QBMessagesAdapter.ViewHolder qbViewHolder;

    private int preferredImageSizePreview = (int) (80 * Resources.getSystem().getDisplayMetrics().density);

    protected LayoutInflater inflater;
    protected Context context;
    protected List<QBChatMessage> chatMessages;
    private RequestListener glideRequestListener;

    private int typeOwnAttachmentMessageLayoutResource = R.layout.item_attachment_message_own;
    private int typeOpponentAttachmentMessageLayoutResource = R.layout.item_attachment_message_opponent;
    private int typeOwnMessageLayoutResource = R.layout.item_text_message_own;
    private int typeOpponentMessageLayoutResource = R.layout.item_text_message_opponent;
    private int typeAttachmentLayoutResource;

    protected int[] viewItemIDs = {R.id.message_textview, R.id.time_text_message_textview, R.id.attach_imageview, R.id.centered_progressbar};
    protected int[] viewItemIDsCustom;
    protected Map<String, Integer> viewItemIDsList;

    public QBMessagesAdapter(Context context, List<QBChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public QBMessagesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewTypes valueType = ViewTypes.values()[viewType];
        switch (valueType) {
            case TYPE_ATTACHMENT_MESSAGE_OWN:
                qbViewHolder = new ViewHolder(inflater.inflate(typeOwnAttachmentMessageLayoutResource, parent, false), valueType, customView, viewItemIDs);
                return qbViewHolder;
            case TYPE_ATTACHMENT_MESSAGE_OPPONENT:
                return new ViewHolder(inflater.inflate(typeOpponentAttachmentMessageLayoutResource, parent, false), valueType, customView, viewItemIDs);
            case TYPE_OWN_MESSAGE:
                return new ViewHolder(inflater.inflate(typeOwnMessageLayoutResource, parent, false), valueType, customView, viewItemIDs);
            case TYPE_OPPONENT_MESSAGE:
                return new ViewHolder(inflater.inflate(typeOpponentMessageLayoutResource, parent, false), valueType, customView, viewItemIDs);
            case TYPE_ATTACHMENT_CUSTOM:
//               resource must be set manually
                return new ViewHolder(inflater.inflate(typeAttachmentLayoutResource, parent, false), valueType, customView, viewItemIDsCustom);
            default:
                return null;
        }
    }

    /**
     * Add id of the ViewHolderItem.
     *
     * @param ids array of IDs in order messageTextView, timeTextMessageTextViewID, attach_imageViewID, attachmentProgressBarID.
     */
    protected void setCustomViewHolderItemIDs(@IdRes int... ids) {
        for (int x = 0; x < ids.length; x++) {
            if (ids[x] != 0) {
                Log.d(TAG, "ids[x]= " + ids[x]);
                viewItemIDs[x] = ids[x];
            }
        }
    }

    /**
     * array of IDs in order messageTextView, timeTextMessageTextViewID, attach_imageViewID, attachmentProgressBarID
     */
    protected int[] setCustomViewItemIDs() {
        return viewItemIDs;
    }

    protected void setTypeOwnMessageLayoutResource(@LayoutRes int typeOwnMessageLayoutResource) {
        this.typeOwnMessageLayoutResource = typeOwnMessageLayoutResource;
    }

    protected void setTypeOpponentMessageLayoutResource(@LayoutRes int typeOpponentMessageLayoutResource) {
        this.typeOpponentMessageLayoutResource = typeOpponentMessageLayoutResource;
    }

    protected void setTypeOwnAttachmentMessageLayoutResource(@LayoutRes int typeOwnAttachmentMessageLayoutResource) {
        this.typeOwnAttachmentMessageLayoutResource = typeOwnAttachmentMessageLayoutResource;
    }

    protected void setTypeOpponentAttachmentMessageLayoutResource(@LayoutRes int typeOpponentAttachmentMessageLayoutResource) {
        this.typeOpponentAttachmentMessageLayoutResource = typeOpponentAttachmentMessageLayoutResource;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        QBChatMessage chatMessage = getItem(position);
        ViewTypes valueType = ViewTypes.values()[getItemViewType(position)];
        switch (valueType) {
            case TYPE_ATTACHMENT_MESSAGE_OWN:
            case TYPE_ATTACHMENT_MESSAGE_OPPONENT:
                initRequestListener(holder);
                showAttachment(holder, chatMessage);
                break;
            case TYPE_OWN_MESSAGE:
            case TYPE_OPPONENT_MESSAGE:
                holder.messageTextView.setText(chatMessage.getBody());
                holder.timeTextMessageTextView.setText(new SimpleDateFormat("MMMM dd", Locale.getDefault()).format(new Date(chatMessage.getDateSent() * 1000)));
                break;
            default:
                break;
        }

    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public QBChatMessage getItem(int position) {
        return chatMessages.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        QBChatMessage chatMessage = getItem(position);

        if (hasAttachments(chatMessage)) {
            QBAttachment attachment = chatMessage.getAttachments().iterator().next();
            Log.d("QBMessagesAdapter", "attachment.getType= " + attachment.getType());
            if (attachment.getType() != null && attachment.getType().equals(QBAttachment.PHOTO_TYPE)) {
                if (isIncoming(chatMessage)) {
                    return ViewTypes.TYPE_ATTACHMENT_MESSAGE_OPPONENT.ordinal();
                } else {
                    return ViewTypes.TYPE_ATTACHMENT_MESSAGE_OWN.ordinal();
                }
            } else {
                return ViewTypes.TYPE_ATTACHMENT_CUSTOM.ordinal();
            }
        } else {
            if (isIncoming(chatMessage)) {
                return ViewTypes.TYPE_OPPONENT_MESSAGE.ordinal();
            } else {
                return ViewTypes.TYPE_OWN_MESSAGE.ordinal();
            }
        }
    }

    @Override
    public void add(QBChatMessage item) {
        chatMessages.add(item);
        notifyDataSetChanged();
    }

    @Override
    public List<QBChatMessage> getList() {
        return chatMessages;
    }

    @Override
    public void addList(List<QBChatMessage> items) {
        chatMessages.addAll(0, items);
        notifyDataSetChanged();
    }

    @Override
    public QBChatMessage getItem(RecyclerView.ViewHolder viewHolder) {
        return null;
    }

    private boolean isIncoming(QBChatMessage chatMessage) {
        QBUser currentUser = QBChatService.getInstance().getUser();
        return chatMessage.getSenderId() != null && !chatMessage.getSenderId().equals(currentUser.getId());
    }

    private boolean hasAttachments(QBChatMessage chatMessage) {
        Collection<QBAttachment> attachments = chatMessage.getAttachments();
        return attachments != null && !attachments.isEmpty();
    }

    private void showAttachment(final ViewHolder holder, QBChatMessage chatMessage) {
        Collection<QBAttachment> attachments = chatMessage.getAttachments();
        QBAttachment attachment = attachments.iterator().next();
        Glide.with(context)
                .load(attachment.getUrl())
                .listener(glideRequestListener)
                .override(preferredImageSizePreview, preferredImageSizePreview)
                .dontTransform()
                .error(R.drawable.ic_error)
                .into(holder.attach_imageView);
    }

    private void initRequestListener(final ViewHolder holder) {
        glideRequestListener = new RequestListener() {
            @Override
            public boolean onException(Exception e, Object model, Target target, boolean isFirstResource) {
                e.printStackTrace();
                holder.attach_imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.attachmentProgressBar.setVisibility(View.GONE);
                return false;
            }

            @Override
            public boolean onResourceReady(Object resource, Object model, Target target, boolean isFromMemoryCache, boolean isFirstResource) {
                holder.attach_imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                holder.attachmentProgressBar.setVisibility(View.GONE);
                return false;
            }
        };
    }

    public void addCustomView(@IdRes int... ids) {
        if (customView == null) {
            customView = new ArrayList<>();
        }
        for (int ID : ids) {
            customView.add(ID);
        }
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {

        TextView messageTextView;
        TextView timeTextMessageTextView;
        ImageView attach_imageView;
        ProgressBar attachmentProgressBar;
        public List<View> listViewBinded = new ArrayList<>();

        public ViewHolder(View itemView, ViewTypes viewType, List<Integer> listView, int... itemIDs) {
            super(itemView);

            for (Integer ID : listView) {
                Log.d(TAG, "ID= " + ID);
                View view = itemView.findViewById(ID);
                listViewBinded.add(view);
            }

            switch (viewType) {
                case TYPE_ATTACHMENT_MESSAGE_OWN:
                case TYPE_ATTACHMENT_MESSAGE_OPPONENT:
                    attach_imageView = (ImageView) itemView.findViewById(itemIDs[2]);
                    attachmentProgressBar = (ProgressBar) itemView.findViewById(itemIDs[3]);
                    break;
                case TYPE_OWN_MESSAGE:
                case TYPE_OPPONENT_MESSAGE:
                    Log.d(TAG, "messageTextView itemIDs[0]= " + itemIDs[0]);
                    messageTextView = (TextView) itemView.findViewById(itemIDs[0]);
                    Log.d(TAG, "messageTextView getId= " + messageTextView.getId());
                    break;
                default:
                    break;
            }
            timeTextMessageTextView = (TextView) itemView.findViewById(itemIDs[1]);
        }

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}