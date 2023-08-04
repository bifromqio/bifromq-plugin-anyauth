# BifroMQ Auth Plugin Development

## What is BifroMQ plugin
[BifroMQ's plugin](https://bifromq.io/docs/plugin/plugin/) mechanism allows users to integrate custom business logic 
with BifroMQ at runtime. Currently, BifroMQ defines 3 types of Plugin interfaces, i.e. Auth Provider, 
Event Collector and Setting Provider.

This repository provides an example for BifroMQ auth provider development. It assumes a use case where users can log in 
to an app through social media accounts such as GMail and WeChat, and receive notifications when messages are published 
through BifroMQ.

In order to reach the goal, not only should IoT devices connect to BifroMQ, but the users do through their social 
media accounts, making the app more accessible.

Also, the auth plugin should check the action permission for coming publishing and subscription messages.
## Auth Plugin Authentication Workflow
Based on the previous discussion, the login process should be done based on the client identity. In the project, WeChat 
and Auth0 authentication is supported. Additionally, it can be extended to support more social media accounts. 
In general, the interactions with third-party services are similar.

The process for each identity authentication is demonstrated as follows:

**Device**

![device-authn.png](docs%2Fimg%2Fdevice-authn.png)

As shown in the above figure, the customized auth service has to query users' credentials from database. In the project,
the interface `IAuthStorage` is provided and `MySql` is implemented based on the interface. One can also extend the 
implementation, e.g. redis and mongo.

Noticing, there is a implicit prerequisite, i.e. the credentials have been store in the database in advance. It can be 
done by other services, which is omitted here.

**Auth0**

![auth0-authn.png](docs%2Fimg%2Fauth0-authn.png)

**WeChat**

![wechat-authn.png](docs%2Fimg%2Fwechat-authn.png)

## Auth Plugin Authorization Workflow
For authorization, the project adopts Access Control List ([ACL](https://en.wikipedia.org/wiki/Access-control_list)).
Each action, such as publishing and subscription, has its own permission rule. For simplicity, the permissions are based 
on each user rather than the client. Therefore, clients under the same user share the same behavior.

In the project, `MySql` is adopted for storing users' permissions in the following table:

| username | pubAcl | subAcl |
|----------|--------|--------|
| "dev"    | "a/b, a/+/c"   | "a/#"  |
| "test"    | "+/b""   | "#"   |

In `pubAcl` and `subAcl`, the rules are comma seperated. Also, being similar to authentication, the rules should be 
stored in advance by other services.

It's important to note that subscription and unsubscription are considered the same action in the auth plugin.

## Notices for Plugin Development
Since BifroMQ's plugin interface implementations are called during the connection and message distribution processes, 
it's crucial to ensure they are lightweight to avoid bottlenecks. Therefore, in the auth plugin, all the work is 
done asynchronously.

Furthermore, based on experience, users' credentials and action permissions do not change frequently. Even during the 
user's lifecycle, the information will remain unchanged. As a result, caching is introduced in the auth service to 
reduce I/O costs.