import { IsUUID, IsPositive } from 'class-validator';

export class CreatePaymentDto {
  @IsUUID()
  orderId!: string;

  @IsPositive()
  amount!: number;
}
