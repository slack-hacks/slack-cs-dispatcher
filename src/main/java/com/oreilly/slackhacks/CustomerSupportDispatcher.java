package com.oreilly.slackhacks;

import com.ullink.slack.simpleslackapi.*;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CustomerSupportDispatcher
{
    private static final String TOKEN = "insert your token here";

    public static void main(String [] args) throws Exception
    {
        //creating the session
        SlackSession session = SlackSessionFactory.createWebSocketSlackSession(TOKEN);
        //adding a message listener to the session
        session.addMessagePostedListener(CustomerSupportDispatcher::processMessagePostedEvent);
        //connecting the session to the Slack team
        session.connect();
        //delegating all the event management to the session
        Thread.sleep(Long.MAX_VALUE);
    }

    private static void processMessagePostedEvent(SlackMessagePosted event, SlackSession session) {
        //filtering all the messages the bot doesn't care of
        if (isNotASmoochMessage(event))
        {
            return;
        }

        //notifying the CS member
        notifyCustomerSupportMember(event, selectCustomerSupportMember(event,session), session );
    }


    private static boolean isNotASmoochMessage(SlackMessagePosted event)
    {
        //if the event is not a bot message
        if (event.getMessageSubType() != SlackMessagePosted.MessageSubType.BOT_MESSAGE) {
            return true;
        }
        //if the sender is not named Smooch
        if (!"Smooch".equals(event.getSender().getUserName())) {
            return true;
        }
        return false;
    }

    private static void notifyCustomerSupportMember(SlackMessagePosted event, SlackUser nominee, SlackSession session)
    {
        String channelReference = getCreatedChannelReference(event);
        session.sendMessage(event.getChannel(), "<@" + nominee.getId() + ">:" + " will handle the issue in " + channelReference);
        session.sendMessageToUser(nominee," Could you please handle the issue in " + channelReference,null);
    }

    private static String getCreatedChannelReference(SlackMessagePosted event) {
        JSONObject json = event.getJsonSource();
        JSONArray array = (JSONArray)json.get("attachments");
        JSONObject attachment = (JSONObject)array.get(0);
        String attachmentHeader = (String)attachment.get("pretext");
        return attachmentHeader.substring(attachmentHeader.lastIndexOf('<'));
    }

    private static SlackUser selectCustomerSupportMember(SlackMessagePosted event, SlackSession session)
    {
        SlackChannel channel  = event.getChannel();
        Collection<SlackUser> users = channel.getMembers();
        List<SlackUser> selectableUsers = users.stream().filter(user -> !user.isBot() && session.getPresence(user) == SlackPersona.SlackPresence.ACTIVE).collect(Collectors.toList());
        return selectableUsers.get((int)Math.random()*selectableUsers.size());
    }


}
