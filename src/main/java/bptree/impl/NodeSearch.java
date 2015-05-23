package bptree.impl;

import bptree.PageProxyCursor;
import org.neo4j.io.pagecache.PagedFile;

import java.io.IOException;


public class NodeSearch {
    private static PrimitiveLongArray arrayUtil = new PrimitiveLongArray();
    public static PageProxyCursor cursor;

    public static SearchCursor find(long[] key){
        long[] entry = null;
        SearchCursor resultsCursor = null;
        int[] searchResult;
        try (PageProxyCursor cursor = NodeTree.disk.getCursor(NodeTree.rootNodeId, PagedFile.PF_EXCLUSIVE_LOCK)) {
                    searchResult = find(cursor, key);
                    long currentNode = cursor.getCurrentPageId();
                    if(searchResult[0] == 0) {
                        int[] altResult = moveCursorBackIfPreviousNodeContainsValidKeys(cursor, key);
                        if (currentNode != cursor.getCurrentPageId()) {
                            searchResult = altResult;
                        }
                    }
                    resultsCursor = new SearchCursor(cursor.getCurrentPageId(), NodeHeader.getSiblingID(cursor), searchResult[0], key, NodeHeader.getKeyLength(cursor), NodeHeader.getNumberOfKeys(cursor));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resultsCursor;
    }

    public static int[] find(PageProxyCursor cursor, long[] key) throws IOException {
        int[] searchResult;
        if(NodeHeader.isLeafNode(cursor)){
            searchResult = search(cursor, key);
        }
        else{
            int index = search(cursor, key)[0];
            long child = NodeTree.getChildIdAtIndex(cursor, index);
            cursor.next(child);
            searchResult = find(cursor, key);
        }
        return searchResult;
    }

    public static int[] search(long nodeId, long[] key) {
        int[] result = new int[]{-1, -1};
        try (PageProxyCursor cursor = NodeTree.disk.getCursor(nodeId, PagedFile.PF_EXCLUSIVE_LOCK)) {
                    result = search(cursor, key);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    public static int[] search(PageProxyCursor cursor, long[] key){
        if(NodeHeader.isLeafNode(cursor)){
            if(NodeHeader.isNodeWithSameLengthKeys(cursor)){
                return searchLeafNodeSameLengthKeys(cursor, key);
            }
            else{
                return searchLeafNodeDifferentLengthKeys(cursor, key);
            }

        }
        else{
            if(NodeHeader.isNodeWithSameLengthKeys(cursor)){
                return searchInternalNodeSameLengthKeys(cursor, key);
            }
            else{
                return searchInternalNodeDifferentLengthKeys(cursor, key);
            }
        }
    }

    private static int[] searchInternalNodeSameLengthKeys(PageProxyCursor cursor, long[] key){
        int index = -1;
        int offset = -1;
        int numberOfKeys = NodeHeader.getNumberOfKeys(cursor);
        if(numberOfKeys == 0){
            return new int[]{0, NodeHeader.NODE_HEADER_LENGTH};
        }
        int keyLength = NodeHeader.getKeyLength(cursor);
        cursor.setOffset(NodeHeader.NODE_HEADER_LENGTH + ((numberOfKeys + 1) * 8)); //header + children
        long[] currKey = new long[keyLength];
        for(int i = 0; i < numberOfKeys; i++){
            for(int j = 0; j < keyLength; j++) {
                currKey[j] = cursor.getLong();
            }
            if(NodeTree.comparator.prefixCompare(key, currKey) < 0){
                index = i;
                offset = cursor.getOffset() - (8 * keyLength);
                break;
            }
        }
        if(index == -1){ //Didn't find anything
            index = numberOfKeys;
            offset = cursor.getOffset();
        }
        return new int[]{index, offset};
    }
    private static int[] searchInternalNodeDifferentLengthKeys(PageProxyCursor cursor, long[] key){
        int index = -1;
        int offset = -1;
        int numberOfKeys = NodeHeader.getNumberOfKeys(cursor);
        int keyLength = NodeHeader.getKeyLength(cursor);
        cursor.setOffset(NodeHeader.NODE_HEADER_LENGTH + ((numberOfKeys + 1) * 8)); //header + children
        long currKey;
        int lastKeyLength; //for rewinding of offset to return.
        for(int i = 0; i < numberOfKeys; i++) {
            lastKeyLength = 1;
            currKey = cursor.getLong();
            while(currKey != Node.KEY_DELIMITER) {
                arrayUtil.put(currKey);
                currKey = cursor.getLong();
                lastKeyLength++;
            }
            if(NodeTree.comparator.prefixCompare(key, arrayUtil.get()) < 0){
                index = i;
                offset = cursor.getOffset() - (8 * lastKeyLength);
                break;
            }
        }
        if(index == -1){ //Didn't find anything
            index = numberOfKeys;
            offset = cursor.getOffset();
        }
        return new int[]{index, offset};
    }

    private static int[] searchLeafNodeSameLengthKeys(PageProxyCursor cursor, long[] key){
        int index = -1;
        int offset = -1;
        int numberOfKeys = cursor.getInt(NodeHeader.BYTE_POSITION_KEY_COUNT);
        int keyLength = NodeHeader.getKeyLength(cursor);
        cursor.setOffset(NodeHeader.NODE_HEADER_LENGTH);
        long[] currKey = new long[keyLength];
        for(int i = 0; i < numberOfKeys; i++){
            for(int j = 0; j < keyLength; j++) {
                currKey[j] = cursor.getLong();
            }
            if(NodeTree.comparator.prefixCompare(key, currKey) <= 0){
                index = i;
                offset = cursor.getOffset() - (8 * keyLength);
                break;
            }
        }
        if(index == -1){ //Didn't find anything
            index = numberOfKeys;
            offset = cursor.getOffset();
        }
        return new int[]{index, offset};
    }

    private static int[] searchLeafNodeDifferentLengthKeys(PageProxyCursor cursor, long[] key){
        int index = -1;
        int offset = -1;
        int numberOfKeys = NodeHeader.getNumberOfKeys(cursor);
        cursor.setOffset(NodeHeader.NODE_HEADER_LENGTH);
        long currKey;
        int lastKeyLength; //for rewinding of offset to return.
        for(int i = 0; i < numberOfKeys; i++) {
            lastKeyLength = 1;
            currKey = cursor.getLong();
            while(currKey != Node.KEY_DELIMITER) {
                arrayUtil.put(currKey);
                currKey = cursor.getLong();
                lastKeyLength++;
            }
            if(NodeTree.comparator.prefixCompare(key, arrayUtil.get()) == 0){
                index = i;
                offset = cursor.getOffset() - (8 * lastKeyLength);
                break;
            }
        }
        if(index == -1){ //Didn't find anything
            index = numberOfKeys;
            offset = cursor.getOffset();
        }
        return new int[]{index, offset};
    }

    private static int[] moveCursorBackIfPreviousNodeContainsValidKeys(PageProxyCursor cursor, long[] key) throws IOException {
        long currentNode = cursor.getCurrentPageId();
        long previousNode = NodeHeader.getPrecedingID(cursor);
        if(previousNode != -1){
            cursor.next(previousNode);
        }
        int[] result = search(cursor, key);
        if(result[0] == NodeHeader.getNumberOfKeys(cursor)){
            cursor.next(currentNode);
        }
        return result;
    }
}