package rocky.raft.utils;

import rocky.raft.dto.Message;

public class MessageUtils {

    public static Message createFailMsg(Message.Sender sender, String reason) {
        Message message = new Message(sender, Message.Type.ERROR);
        message.setStatus(Message.Status.FAIL);
        message.setMessage(reason);
        return message;
    }
}
