import { IsNotEmpty, IsNumber, IsUUID, IsPositive } from 'class-validator';

export class CreatePaymentDto {
  @IsUUID()
  @IsNotEmpty()
  orderId!: string;

  @IsNumber()
  @IsPositive()
  amount!: number;
}