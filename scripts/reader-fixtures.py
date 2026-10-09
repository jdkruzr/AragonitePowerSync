"""Fail-closed reading extensions for the existing synthetic lab sync function."""
import json
import re


def extend_sync(original, book_ids, native_assets=None, dynamic_native_books=()):
    if not book_ids or len(set(book_ids)) != len(book_ids) or any(
        not re.fullmatch(r'[0-9a-f-]{32,36}', value) for value in book_ids):
        raise ValueError('Require explicit unique synthetic book UUIDs')
    native_assets = native_assets or {}
    if not isinstance(native_assets, dict) or len(native_assets) > 64 or any(
        book not in book_ids or not isinstance(key, str) or
        not re.fullmatch(r'powersync-lab/reader/' + re.escape(book) +
                         r'/(?:point|shape|resource/pb|resource/data)/[A-Za-z0-9#._-]+', key)
        for key, book in native_assets.items()):
        raise ValueError('Require exact native assets within enrolled reader books')
    if (not isinstance(dynamic_native_books, (list, tuple)) or
            len(set(dynamic_native_books)) != len(dynamic_native_books) or
            any(book not in book_ids for book in dynamic_native_books)):
        raise ValueError('Dynamic native keys require explicit enrolled books')
    opening = original.find('{')
    if opening < 0 or not original.lstrip().startswith('function('):
        raise ValueError('Expected the original lab sync function')
    guard = r'''
 var readerBooks = %s;
 var readerNativeAssets = %s;
 var readerDynamicBooks = %s;
 var readerUser = doc.user || (oldDoc && oldDoc.user);
 var readerScope = doc.dbId || (oldDoc && oldDoc.dbId);
 var readerId = doc.uniqueId || (oldDoc && oldDoc.uniqueId);
 var readerParent = doc.documentId || doc.documentUniqueId ||
                    (oldDoc && (oldDoc.documentId || oldDoc.documentUniqueId));
 var readerMode = doc.modeType || (oldDoc && oldDoc.modeType);
 if (readerUser === "powersync-lab") {
   if (doc.type === "labAsset" && readerBooks.indexOf(readerParent) >= 0 &&
       readerScope === "powersync-lab-ASSET-" + readerParent &&
       (doc.ossKey === "powersync-lab/reader/" + readerParent + "/book/" + readerParent + ".epub" ||
        doc.ossKey === "powersync-lab/reader/" + readerParent + "/book/" + readerParent + ".pdf" ||
        doc.ossKey === "powersync-lab/reader/" + readerParent + "/thumbnail/" + readerParent + ".png" ||
        readerNativeAssets[doc.ossKey] === readerParent ||
        (readerDynamicBooks.indexOf(readerParent) >= 0 && typeof doc.ossKey === "string" &&
         doc.ossKey.indexOf("powersync-lab/reader/" + readerParent + "/") === 0 &&
         /^(point\/[A-Za-z0-9#_-]+#points|shape\/[A-Za-z0-9#_-]+\.zip)$/.test(
           doc.ossKey.substring(("powersync-lab/reader/" + readerParent + "/").length))))) {
     requireAccess(readerScope); channel(readerScope); return;
   }
   if (readerScope === "powersync-lab-READER_LIBRARY" &&
       ((readerMode === 4 && readerBooks.indexOf(readerId) >= 0 && doc._id === "powersync-lab#" + readerId) ||
        ((readerMode === 1 || readerMode === 2 || readerMode === 7) &&
         readerBooks.indexOf(readerParent) >= 0 && doc._id === "powersync-lab#" + readerId) ||
        (doc.commitType === 4 && doc.commitStatus === 1 &&
         readerBooks.indexOf(readerParent) >= 0 &&
         (doc._id === readerId || doc._id === "powersync-lab#" + readerId)))) {
     requireAccess(readerScope); channel(readerScope); return;
   }
   if (readerBooks.indexOf(readerParent) >= 0 &&
       readerScope === "powersync-lab-" + readerParent &&
       (readerMode === 1 || readerMode === 2 || readerMode === 7)) {
     requireAccess(readerScope); channel(readerScope); return;
   }
   if (readerScope === "powersync-lab-MESSAGE" && doc.msgType === 6 &&
       doc.replicatorName === "READER_LIBRARY" && !doc._deleted &&
       typeof doc.syncAt === "number") {
     requireAccess(readerScope); channel(readerScope); return;
   }
 }
 ''' % (json.dumps(book_ids), json.dumps(native_assets), json.dumps(dynamic_native_books))
    return original[:opening+1] + guard + original[opening+1:]
