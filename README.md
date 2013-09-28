zapposNotifier
==============

A java standalone price update notifier for Zappos api


Solution :
Zappos Notifier is a standalone jar which collects customer email and product ids in csv file format,
makes api calls to Zappos API for the product prices/discounts & notifies the customer via email 
once 20% discount is reached.

Dependencies:
- Apache HttpClient
- QuickJsonParser
- JavaMail

Configurations required:
The jar requires the admin to set up in config.properties the following fields before the jar is executed
1. username & password - for sending email
2. customer.csv - path/to/input/customer/information/file
3. poll-interval - interval between 2 successive api calls (in seconds)
4. smtp.host & smtp.port - smtp details to send email
5. senderid  - mail sender of all notification emails
