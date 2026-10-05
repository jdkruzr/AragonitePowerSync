package com.onyx.android.sdk.note;
import com.onyx.android.sdk.note.NoteContentIntentCallback;
interface INoteContentService {
 oneway void sendMessageAction(in Bundle bundle);
 oneway void loadVirtualPageIdList(String docId, NoteContentIntentCallback callback);
}
