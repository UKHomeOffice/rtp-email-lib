# RTP Email Library - Scala library to work with Emails

Sending email requires opening network connections to an SMTP server or GovNotify. To handle network failures gracefully we need to implement retry logic and persistent queues during app restarts. We also have other challenges such as generating HTML content dynamically and managing it.

Our large applications instead write emails they want to send to a mongo db collection and [rt-emailer](https://github.com/UKHomeOffice/rt-emailer), a background program, picks them up and handles all those challenges. The result is that our business logic is simpler and less likely to run into technical issues since sending an email has been reduced to simply writing a row to the database.

This library describes the shared Email class used by various services.
