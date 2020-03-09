package mcping;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

class Pinger
{
    private InetSocketAddress host;
    private int timeout;

    void setAddress(InetSocketAddress host)
    {
        this.host = host;
    }
    void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    private int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        int k;
        do
        {
            k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) {
                //throw new RuntimeException("VarInt too big");
                return -1;
            }
        } while ((k & 0x80) == 128);
        return i;
    }

    private void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        for (;;)
        {
            if ((paramInt & 0xFFFFFF80) == 0)
            {
                out.writeByte(paramInt);
                return;
            }
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    public String fetchData() throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(this.timeout);
        socket.connect(this.host, this.timeout);
        OutputStream outputStream = socket.getOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        InputStream inputStream = socket.getInputStream();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream handshake = new DataOutputStream(b);
        handshake.writeByte(0);
        writeVarInt(handshake, 4);
        writeVarInt(handshake, this.host.getHostString().length());
        handshake.writeBytes(this.host.getHostString());
        handshake.writeShort(this.host.getPort());
        writeVarInt(handshake, 1);
        writeVarInt(dataOutputStream, b.size());
        dataOutputStream.write(b.toByteArray());
        dataOutputStream.writeByte(1);
        dataOutputStream.writeByte(0);
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        int size = readVarInt(dataInputStream);
        if(size == -1) return null;
        int id = readVarInt(dataInputStream);
        if (id == -1) {
            return null;
        }
        if (id != 0) {
            return null;
        }
        int length = readVarInt(dataInputStream);
        if(length == -1) {
            return null;
        }
        if (length == 0) {
            return null;
        }
        byte[] in = new byte[length];
        dataInputStream.readFully(in);

        b.close();
        dataInputStream.close();
        handshake.close();
        dataOutputStream.close();
        outputStream.close();
        inputStream.close();
        socket.close();
        return new String(in);  //ritorna il json
    }
}
