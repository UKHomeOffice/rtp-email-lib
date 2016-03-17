package cjp.emailer

case class ImapConfig(server:String = "",
                      port:String = "",
                      protocol:String = "",
                      user:String = "",
                      password:String = "")