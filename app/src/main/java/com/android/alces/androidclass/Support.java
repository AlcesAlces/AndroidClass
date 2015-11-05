package com.android.alces.androidclass;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class Support {

    public static class Files
    {
        public static byte[] toByteArray(File file) throws IOException
        {
            // Open file
            RandomAccessFile f = new RandomAccessFile(file, "r");
            try {
                // Get and check length
                long longlength = f.length();
                int length = (int) longlength;
                if (length != longlength)
                    throw new IOException("File size >= 2 GB");
                // Read file and return data
                byte[] data = new byte[length];
                f.readFully(data);
                return data;
            } finally {
                f.close();
            }
        }

        public static void saveFileFromBytes(byte[] bytes, String filename) throws IOException
        {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filename));
            bos.write(bytes);
            bos.flush();
            bos.close();
        }
    }

    public static class Connection
    {
        public static void checkConnection()
        {
            if(!Global.globalSocket.connected())
            {
                Global.globalSocket.connect();
            }

        }
    }

    public static class Users
    {
        public static UserCompact findUserByName(String name, ArrayList<UserCompact> users)
        {

            UserCompact toReturn = null;

            for(UserCompact uc : users)
            {
                if(name.toLowerCase().equals(uc.name.toLowerCase()))
                {
                    toReturn = uc.makeClone();
                }
            }

            return toReturn;
        }

        public static void setAllNotBroadcasting(ArrayList<UserCompact> users)
        {
            for(UserCompact uc : users)
            {
                uc.broadcasting = false;
            }
        }

        public static void setBroadcasterById(ArrayList<UserCompact> users, String id)
        {
            for(UserCompact uc : users)
            {
                if(uc.id.equals(id))
                {
                    uc.broadcasting = true;
                }
            }
        }
    }
}
