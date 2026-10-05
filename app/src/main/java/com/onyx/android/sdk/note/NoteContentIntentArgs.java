package com.onyx.android.sdk.note;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;

/** Reconstructed wire layout, verified against Lumi Notes 45256; no vendor implementation. */
public final class NoteContentIntentArgs implements Parcelable {
 public final String docId;
 public final String content;
 public final ParcelFileDescriptor pfd;
 private NoteContentIntentArgs(Parcel in) {
  docId = in.readString(); content = in.readString();
  pfd = in.readParcelable(ParcelFileDescriptor.class.getClassLoader());
 }
 public void writeToParcel(Parcel out, int flags) {
  out.writeString(docId); out.writeString(content); out.writeParcelable(pfd, flags);
 }
 public int describeContents() { return pfd == null ? 0 : CONTENTS_FILE_DESCRIPTOR; }
 public static final Creator<NoteContentIntentArgs> CREATOR = new Creator<>() {
  public NoteContentIntentArgs createFromParcel(Parcel in) { return new NoteContentIntentArgs(in); }
  public NoteContentIntentArgs[] newArray(int n) { return new NoteContentIntentArgs[n]; }
 };
}
