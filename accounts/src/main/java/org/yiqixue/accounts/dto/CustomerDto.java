package org.yiqixue.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(
        name="Customer",
        description = "Schema 数据框架用来存储客户与账户数据"
)
public class CustomerDto {

    @Schema(
//            name="Customer 客户数据姓名字段",
            description = "客户的姓名字段，显示客户的姓名，长度5到30个字符",
            example="扎西德勒"
    )

    @NotEmpty(message="Name cannot be null or empty.")
    @Size(min=5, max=30, message="The length of the customer name should be between 5 and 30")
    private String name;

    @Schema(
            description = "Email address of the customer", example = "tutor@eazybytes.com"
    )
    @NotEmpty(message="Email Address can not be null or empty")
    @Email(message="Email address shoudl be a valid value")
    private String email;

    @Schema(
            description = "Mobile Number of the customer", example = "9345432123"
    )

    @Pattern(regexp="(^$|[0-9]{10})", message = "mobile number must be 10 digit")
    private String mobileNumber;

    private AccountsDto accountsDto;

}
