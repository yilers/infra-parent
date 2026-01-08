package com.github.yilers.upm.handler;

public class EmailHandler {

    public void init() {

    }

//    public static void main(String[] args) throws Exception {
//        MailSmtpConfig.MailSmtpConfigBuilder builder = MailSmtpConfig.builder();
//        builder.username("stefan95@qq.com");
//        builder.password("skavodzzulksbeed");
//        builder.smtpServer("smtp.qq.com");
//        builder.port("465");
//        builder.fromAddress("695452483@qq.com");
//        builder.nickName("jzy");
//        MailSmtpConfig config = builder.build();
//        MailFactory.put("qq", config);
//
//
//        MailClient qqMailClient = MailFactory.createMailClient("qq");
//        Map<String, String> map = new HashMap<>();
//        map.put("title", "登录验证码");
//        map.put("username", "张三");
//        map.put("code", "952789");
//        map.put("minutes", "5");
//        map.put("actionUrl", "https://baidu.com");
//        map.put("message", "因业务需要 需要对你的数据进行回收 请及时登录系统 查看最新变化");
//        MailMessage message = MailMessage.Builder()
//                //收件人地址，此处可以为字符串的单个地址也可以为一个List<String>的群发地址
//                //例如  .mailAddress (new ArrayList<String>().add("XXXXXXXXX@qq.com"))
//                .mailAddress("695452483@qq.com")
//                // 邮件的标题
//                .title("测试标题")
//                // 邮件的文字正文，但是如果发送html邮件时候，大多数的邮件服务商会隐藏掉body的信息
//                .body("测试邮件发送")
//                //html模板来源，此处与mailAddress相似可以为一个字符串形式的html文件名，此时默认读取resources/template目录下的文件
//                //也可以为一个 InputStream 输入流
//                //例如 .html(Files.newInputStream(new File("D:\\index.html").toPath()))
//                .html(Files.newInputStream(new File("/Users/jzy/IdeaProjects/infra-parent-service/upm/upm-core/src/main/resources/template/verify.html").toPath()))
//                /* html模板变量，如果你的html不需要变量，则可以为空，这里有多种的用法
//                Map map的key为变量名称，map的value为变量的值
//                例如 Map<String,String> map = new HashMap<String,String>().put("变量名称","变量值");    .htmlValues(map)
//                还可以直接写入变量名称和变量值 如.htmlValues("变量名称",“变量值”)
//                此处也可以使用接口进行，接口形式在下方详细介绍
//                */
//
//                .htmlValues(map)
//                //抄送人，可以添加一个或多个，使用方式与.mailAddress一致
////                .cc("xxxxxx@qq.com")
//                // 密送人，可以添加一个或多个，使用方式与.mailAddress一致
//
////                .bcc("xxxxxx@qq.com")
//                // 附件，此处与.htmlValues使用方法相似，如果你只有一个附件，可以直接填写key value 如果你有多个附件，这里可以接受一个map
////                .files(“文件名”,"文件路径")
//                //3.0版本支持发送文件自动压缩，你可以选择指定这里的压缩文件名称，框架将会自动将 .files中的所有文件打为一个压缩包并发送
////                .zipName("压缩文件名称")
//                .build();
//        qqMailClient.send(message);
//
//    }
}
