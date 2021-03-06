package com.cloud.storage.snapshot;

import com.cloud.engine.subsystem.api.storage.SnapshotInfo;
import com.cloud.engine.subsystem.api.storage.VolumeInfo;
import com.cloud.legacymodel.communication.answer.Answer;
import com.cloud.legacymodel.communication.command.Command;
import com.cloud.legacymodel.exceptions.ResourceAllocationException;
import com.cloud.legacymodel.storage.Volume;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;

/**
 *
 *
 */
public interface SnapshotManager {

    public static final int HOURLYMAX = 8;
    public static final int DAILYMAX = 8;
    public static final int WEEKLYMAX = 8;
    public static final int MONTHLYMAX = 12;
    public static final int DELTAMAX = 16;

    /**
     * For each of the volumes in the account, (which can span across multiple zones and multiple secondary storages), delete
     * the dir on the secondary storage which contains the backed up snapshots for that volume. This is called during
     * deleteAccount.
     *
     * @param accountId The account which is to be deleted.
     */
    boolean deleteSnapshotDirsForAccount(long accountId);

    String getSecondaryStorageURL(SnapshotVO snapshot);

    //void deleteSnapshotsDirForVolume(String secondaryStoragePoolUrl, Long dcId, Long accountId, Long volumeId);

    boolean canOperateOnVolume(Volume volume);

    Answer sendToPool(Volume vol, Command cmd);

    SnapshotVO getParentSnapshot(VolumeInfo volume);

    Snapshot backupSnapshot(Long snapshotId);

    SnapshotInfo takeSnapshot(VolumeInfo volume) throws ResourceAllocationException;
}
